# LeoSync – Help

LeoSync backs up photos, videos and any folder from your mobile to your Windows PC – directly on your home network, without a cloud and without an account. Files only travel between mobile and PC.

## 1. On the PC: share a folder

1. In Explorer, right-click the target folder (e.g. `D:\Photos`) → **Properties** → **Sharing** → **Advanced Sharing**.
2. Tick **Share this folder** and give it a Share name (e.g. `Photos`).
3. Under **Permissions**, allow **Change** and **Read** for your Windows user.
4. Find the PC's IP address with `ipconfig` in the command prompt (e.g. `192.168.1.20`). Ideally give the PC a fixed address in your router.

## 2. In LeoSync: create a profile

Tap **Add profile** and choose the type:

- **Back up photos & videos** – from the camera to the PC, sorted automatically by year, month and travel country (e.g. `2026\06 - Egypt`). Optionally remove them from the mobile afterwards.
- **Sync a folder** – any folder 1:1, in one or both directions.

The wizard guides you step by step through name, source, target and options. For the target, the PC is enough (`\\192.168.1.20`) plus user and password; **Choose folder on the PC…** then lists the Shares and folders. **Test connection** shows right away whether everything is correct.

New profiles only start after you tap **Sync** once – you can **simulate** first what would happen.

## 3. Day to day

- **Automatic:** As soon as the mobile is connected to one of the entered Wi-Fi networks, LeoSync syncs in the background. Without a Wi-Fi name only when you trigger it.
- **Manual:** Tap **Sync** or pull down at the very top of the home screen.
- **Pause:** ⏸ at the top right – for a while or "forever" (manual only). ▶ resumes.
- **The dog** shows the state: sleeping = everything backed up, running = sync in progress, standing = waiting for your decision, sitting = waiting (e.g. for Wi-Fi).

## 4. Keeping your files safe

- **Check after upload:** LeoSync reads the file back from the PC and compares it with the original. Levels from *Off* to *Complete*; default is *Medium*. If photos are removed from the mobile, a check is always done.
- **Trash:** For folder profiles, deleted and overwritten files go to the `.LeoSync-Bin` folder (on mobile and PC) and can be restored via **Tools → Trash**. It is emptied after 30 days (adjustable).
- **Safety brake:** If many files would be deleted at once (default: more than 20 %) or a whole folder is missing, LeoSync stops and asks you first.
- **Conflicts:** If the same file was changed on mobile and PC, you decide which version wins. The other one goes to the trash.

## 5. Common messages

| Message | What to do |
|---|---|
| User name or password is wrong | Check the Windows user and password. For a Microsoft account use the e-mail address as user. |
| This Share does not exist on the PC | Check the Share name or pick it with **Choose folder on the PC…**. |
| No access to this folder | Allow **Change** for the user in the Share permissions. |
| The PC is not reachable | Is the PC switched on (not asleep)? Is the mobile on the same Wi-Fi? Does the firewall allow "File and Printer Sharing"? |
| The PC name was not found | Enter the IP address instead of the name, e.g. `\\192.168.1.20`. |
| PC folder not found (safety brake) | Was the folder renamed or moved? Rename it back and tap **Check again** – or choose a new folder. |

## 6. Sync stops in the background

Many manufacturers close apps in the background very aggressively. In LeoSync under **Settings → App permissions** you find the steps for your device (battery optimization off, allow background activity). More: [dontkillmyapp.com](https://dontkillmyapp.com).

## 7. Privacy

LeoSync has no server and collects no data. Passwords are stored encrypted on the mobile (Android Keystore). Diagnostic data only leaves the mobile if you share it yourself.

## Contact

Questions or problems: [leotools@protonmail.com](mailto:leotools@protonmail.com)
