#!/usr/bin/env python3
"""Lokaler SMB-Testserver fuer LeoSync-Tests im Emulator (nur Testdaten!).

Startet mit impacket einen SMB2-Server, der einen Ordner als Freigabe anbietet. Wird von
tools/qa-smb-testshare.ps1 gestartet (venv, adb reverse); Aufruf von Hand:
    python qa-smb-testshare.py --dir <Ordner> [--share QA] [--user qa] [--password qa-test-1] [--port 4445]

Hinweis: impacket meldet immer dieselbe Server-GUID ("AAAAAAAAAAAAAAAA"); fuer Tests mit einer
anderen PC-Kennung kann --guid (16 Zeichen) gesetzt werden.
"""
import argparse
import logging
import os
import sys

from impacket import ntlm, smbserver


def main() -> int:
    ap = argparse.ArgumentParser(description="SMB-Testfreigabe fuer LeoSync")
    ap.add_argument("--dir", required=True, help="Ordner, der als Freigabe bereitgestellt wird")
    ap.add_argument("--share", default="QA", help="Freigabename (Standard QA)")
    ap.add_argument("--user", default="qa", help="Benutzer (Standard qa)")
    ap.add_argument("--password", default="qa-test-1", help="Testpasswort (nur Testdaten)")
    ap.add_argument("--port", type=int, default=4445, help="lokaler Port (Standard 4445; Windows belegt 445)")
    ap.add_argument("--address", default="127.0.0.1", help="Bindeadresse (Standard nur localhost)")
    ap.add_argument("--guid", default="", help="optional: 16 Zeichen Server-GUID statt AAAA...")
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
