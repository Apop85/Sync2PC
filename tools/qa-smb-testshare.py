#!/usr/bin/env python3
"""Lokaler SMB-Testserver fuer LeoSync-Tests im Emulator (nur Testdaten!).

Startet mit impacket einen SMB2-Server, der einen Ordner als Freigabe anbietet. Wird von
tools/qa-smb-testshare.ps1 gestartet (venv, adb reverse); Aufruf von Hand:
    python qa-smb-testshare.py --dir <Ordner> [--share QA] [--user qa] [--password qa-test-1] [--port 4445]
        [--throttle-kbps n] [--drop-after-bytes n [--drop-count k]]

Hinweis: impacket meldet immer dieselbe Server-GUID ("AAAAAAAAAAAAAAAA"); fuer Tests mit einer
anderen PC-Kennung kann --guid (16 Zeichen) gesetzt werden.
"""
import argparse
import logging
import os
import sys
import threading
import time

from impacket import ntlm, smbserver
from impacket import smb3structs as smb2
from impacket.nt_errors import (
    STATUS_END_OF_FILE, STATUS_INVALID_HANDLE, STATUS_NO_SUCH_FILE, STATUS_OBJECT_NAME_COLLISION,
    STATUS_OBJECT_NAME_NOT_FOUND, STATUS_OBJECT_PATH_SYNTAX_BAD, STATUS_ACCESS_DENIED, STATUS_SUCCESS,
)

STATUS_SMB_BAD_TID = smbserver.STATUS_SMB_BAD_TID


def patch_windows_behaviour() -> None:
    """Gleicht vier Abweichungen von impacket gegenueber Windows aus (SYNC-20261111-165), damit Upload, Rename,
    Loeschen, Fortsetzen und Verbindungsabbruch der Ordner-Spiegelung pruefbar sind. Nur Testserver, nie produktiv.

    1. Oeffnen einer nicht vorhandenen Datei: STATUS_OBJECT_NAME_NOT_FOUND statt STATUS_NO_SUCH_FILE
       (smbj meldet sonst "Create failed", die App kann "Datei fehlt" nicht erkennen).
    2. Lesen am Dateiende: STATUS_END_OF_FILE mit Fehler-Antwort (SMB2Error) statt einer Read-Antwort
       (smbj: BufferException Underflow, der Lauf haengt im Logoff bis zum Timeout).
    3. Rename (SetInfo FileRenameInformation): Handle vor dem Umbenennen schliessen, os.replace statt os.rename,
       danach neu oeffnen (unter Windows scheitert os.rename bei offenem Handle mit WinError 32).
    4. Handles abgebrochener Verbindungen schliessen (sonst bleibt die Datei bis zum Serverneustart gesperrt).
    """
    cmds = smbserver.SMB2Commands

    # 1. Fehlercode beim Oeffnen
    orig_create = cmds.smb2Create

    def create(connId, smbServer, recvPacket):
        resp, extra, code = orig_create(connId, smbServer, recvPacket)
        if code == STATUS_NO_SUCH_FILE:
            code = STATUS_OBJECT_NAME_NOT_FOUND
        return resp, extra, code

    cmds.smb2Create = staticmethod(create)

    # 2. Dateiende als Fehler-Antwort
    orig_read = cmds.smb2Read

    def read(connId, smbServer, recvPacket):
        resp, extra, code = orig_read(connId, smbServer, recvPacket)
        if code == STATUS_END_OF_FILE:
            return [smb2.SMB2Error()], extra, code
        return resp, extra, code

    cmds.smb2Read = staticmethod(read)

    # 3. Rename mit geschlossenem Handle
    orig_setinfo = cmds.smb2SetInfo

    def setinfo(connId, smbServer, recvPacket):
        try:
            info = smb2.SMB2SetInfo(recvPacket["Data"])
            is_rename = info["InfoType"] == smb2.SMB2_0_INFO_FILE and info["FileInfoClass"] == smb2.SMB2_FILE_RENAME_INFO
        except Exception:
            is_rename = False
        if not is_rename:
            return orig_setinfo(connId, smbServer, recvPacket)
        return rename(connId, smbServer, recvPacket, info)

    def rename(connId, smbServer, recvPacket, info):
        conn = smbServer.getConnectionData(connId)
        file_id = info["FileID"].getData()
        if file_id == b"\xff" * 16 and "SMB2_CREATE" in conn["LastRequest"]:
            file_id = conn["LastRequest"]["SMB2_CREATE"]["FileID"]
        tid = recvPacket["TreeID"]
        if tid not in conn["ConnectedShares"]:
            return [smb2.SMB2Error()], None, STATUS_SMB_BAD_TID
        share = conn["ConnectedShares"][tid]
        if file_id not in conn["OpenedFiles"]:
            return [smb2.SMB2Error()], None, STATUS_INVALID_HANDLE
        opened = conn["OpenedFiles"][file_id]
        root = share["path"]
        rinfo = smb2.FILE_RENAME_INFORMATION_TYPE_2(info["Buffer"])
        new_name = smbserver.normalize_path(rinfo["FileName"].decode("utf-16le"))
        new_path = os.path.join(root, new_name)
        if not smbserver.isInFileJail(root, new_name):
            return [smb2.SMB2Error()], None, STATUS_OBJECT_PATH_SYNTAX_BAD
        if rinfo["ReplaceIfExists"] == 0 and os.path.exists(new_path):
            return [smb2.SMB2Error()], None, STATUS_OBJECT_NAME_COLLISION
        if share["read only"] == "yes":
            return [smb2.SMB2Error()], None, STATUS_ACCESS_DENIED
        old_path = opened["FileName"]
        handle = opened["FileHandle"]
        had_handle = handle not in (smbserver.VOID_FILE_DESCRIPTOR, smbserver.PIPE_FILE_DESCRIPTOR)
        try:
            if had_handle:
                os.close(handle)
            os.replace(old_path, new_path)
            opened["FileName"] = new_path
            if had_handle:
                opened["FileHandle"] = os.open(new_path, os.O_RDWR | getattr(os, "O_BINARY", 0))
        except Exception as e:
            smbServer.log("smb2SetInfo(rename): %s" % e, logging.ERROR, connData=conn)
            # Handle moeglichst wiederherstellen, damit das spaetere Close nicht ins Leere greift
            if had_handle:
                try:
                    opened["FileHandle"] = os.open(opened["FileName"], os.O_RDWR | getattr(os, "O_BINARY", 0))
                except Exception:
                    opened["FileHandle"] = smbserver.VOID_FILE_DESCRIPTOR
            return [smb2.SMB2Error()], None, STATUS_ACCESS_DENIED
        smbServer.setConnectionData(connId, conn)
        return [smb2.SMB2SetInfo_Response()], None, STATUS_SUCCESS

    cmds.smb2SetInfo = staticmethod(setinfo)

    # 4. Handles beim Ende einer Verbindung schliessen (auch bei Abbruch)
    orig_remove = smbserver.SMBSERVER.removeConnection

    def remove_connection(self, name):
        try:
            conn = self.getConnectionData(name, checkStatus=False)
            for opened in list(conn.get("OpenedFiles", {}).values()):
                handle = opened.get("FileHandle")
                if handle not in (None, smbserver.VOID_FILE_DESCRIPTOR, smbserver.PIPE_FILE_DESCRIPTOR):
                    try:
                        os.close(handle)
                    except Exception:
                        pass
                if opened.get("DeleteOnClose") is True:
                    try:
                        os.remove(opened["FileName"])
                    except Exception:
                        pass
        except Exception:
            pass
        return orig_remove(self, name)

    smbserver.SMBSERVER.removeConnection = remove_connection


def patch_throttle_and_drop(throttle_kbps: int, drop_after_bytes: int, drop_count: int) -> None:
    """Testhilfen fuer reproduzierbare Abbrueche (SYNC-20261111-196), nur Testserver.

    --throttle-kbps n:      Lesen und Schreiben der Dateidaten auf etwa n KB/s bremsen (Pause nach jedem Datenblock),
                            damit eine Dateiaenderung waehrend des Laufs oder ein manueller Abbruch gezielt moeglich ist.
    --drop-after-bytes n:   Verbindung hart trennen (ohne Antwort, wie ein Netzabbruch), sobald auf dieser Verbindung
                            n Bytes Dateidaten uebertragen wurden (Lesen + Schreiben).
    --drop-count k:         nur die ersten k Verbindungen trennen (Standard 1), damit der Wiederholungsversuch der App durchlaeuft.
    """
    cmds = smbserver.SMB2Commands
    lock = threading.Lock()
    transferred = {}   # connId -> Bytes
    doomed = set()     # connIds, die nach der laufenden Anfrage getrennt werden
    state = {"dropped": 0}

    def account(conn_id, nbytes):
        if nbytes > 0 and throttle_kbps > 0:
            time.sleep(nbytes / (throttle_kbps * 1024.0))
        if drop_after_bytes > 0 and nbytes > 0:
            with lock:
                transferred[conn_id] = transferred.get(conn_id, 0) + nbytes
                if transferred[conn_id] >= drop_after_bytes and state["dropped"] < drop_count and conn_id not in doomed:
                    doomed.add(conn_id)
                    state["dropped"] += 1
                    print(f"[test] Verbindung {conn_id} wird nach {transferred[conn_id]} Bytes getrennt "
                          f"({state['dropped']}/{drop_count})", flush=True)

    orig_write = cmds.smb2Write
    orig_read = cmds.smb2Read

    def write(connId, smbServer, recvPacket):
        try:
            n = int(smb2.SMB2Write(recvPacket["Data"])["Length"])
        except Exception:
            n = 0
        account(connId, n)
        return orig_write(connId, smbServer, recvPacket)

    def read(connId, smbServer, recvPacket):
        resp, extra, code = orig_read(connId, smbServer, recvPacket)
        try:
            n = int(resp[0]["DataLength"]) if code == STATUS_SUCCESS else 0
        except Exception:
            n = 0
        account(connId, n)
        return resp, extra, code

    cmds.smb2Write = staticmethod(write)
    cmds.smb2Read = staticmethod(read)

    # Trennen: Ausnahme nach der Anfrage, dann beendet impacket die Verbindung ohne Antwort (Handler bricht ab)
    orig_process = smbserver.SMBSERVER.processRequest

    def process(self, connId, data):
        resp = orig_process(self, connId, data)
        if connId in doomed:
            doomed.discard(connId)
            raise ConnectionAbortedError("Testabbruch (--drop-after-bytes)")
        return resp

    smbserver.SMBSERVER.processRequest = process

def main() -> int:
    ap = argparse.ArgumentParser(description="SMB-Testfreigabe fuer LeoSync")
    ap.add_argument("--dir", required=True, help="Ordner, der als Freigabe bereitgestellt wird")
    ap.add_argument("--share", default="QA", help="Freigabename (Standard QA)")
    ap.add_argument("--user", default="qa", help="Benutzer (Standard qa)")
    ap.add_argument("--password", default="qa-test-1", help="Testpasswort (nur Testdaten)")
    ap.add_argument("--port", type=int, default=4445, help="lokaler Port (Standard 4445; Windows belegt 445)")
    ap.add_argument("--address", default="127.0.0.1", help="Bindeadresse (Standard nur localhost)")
    ap.add_argument("--guid", default="", help="optional: 16 Zeichen Server-GUID statt AAAA...")
    ap.add_argument("--throttle-kbps", type=int, default=0, help="Datenrate in KB/s begrenzen (0 = unbegrenzt)")
    ap.add_argument("--drop-after-bytes", type=int, default=0, help="Verbindung nach n uebertragenen Bytes trennen (0 = aus)")
    ap.add_argument("--drop-count", type=int, default=1, help="nur die ersten k Verbindungen trennen (Standard 1)")
    args = ap.parse_args()

    os.makedirs(args.dir, exist_ok=True)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(message)s")

    if args.guid:
        if len(args.guid) != 16:
            print("--guid braucht genau 16 Zeichen", file=sys.stderr)
            return 2
        # impacket setzt die GUID fest im Modul (smbserver.py): hier zur Laufzeit ersetzen
        guid = args.guid.encode("ascii")
        orig = smbserver.SMB2Commands.smb2Negotiate

        def negotiate(connId, smbServer, recvPacket, isSMB1=False):
            cmd = orig(connId, smbServer, recvPacket, isSMB1)
            try:
                resp = cmd[0] if isinstance(cmd, tuple) else cmd
                resp["ServerGuid"] = guid
            except Exception:
                pass
            return cmd

        smbserver.SMB2Commands.smb2Negotiate = staticmethod(negotiate)

    patch_windows_behaviour()
    if args.throttle_kbps > 0 or args.drop_after_bytes > 0:
        patch_throttle_and_drop(args.throttle_kbps, args.drop_after_bytes, args.drop_count)
    server = smbserver.SimpleSMBServer(listenAddress=args.address, listenPort=args.port)
    server.addShare(args.share.upper(), os.path.abspath(args.dir), "LeoSync-Test")
    server.setSMB2Support(True)
    nthash = ntlm.compute_nthash(args.password).hex()
    server.addCredential(args.user, 1000, "aad3b435b51404eeaad3b435b51404ee", nthash)
    server.setLogFile("")
    print(f"SMB-Testserver auf {args.address}:{args.port}, Freigabe {args.share.upper()} -> {os.path.abspath(args.dir)}, Benutzer {args.user}")
    server.start()
    return 0


if __name__ == "__main__":
    sys.exit(main())
