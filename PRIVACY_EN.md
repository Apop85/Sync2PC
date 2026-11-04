# Privacy Policy – LeoSync

**Last updated:** November 5, 2026  
**Version:** 1.0

This privacy policy explains what data is processed by the LeoSync Android application and how this data is handled.

LeoSync is an application developed under the Leo brand. Its purpose is to synchronize photos and videos between Android devices and users' own network shares without relying on external cloud services.

## 1. Data Controller

The party responsible for data processing is:

- **Developer:** [Full legal name]
- **Country:** Switzerland
- **Contact:** [Email address]

## 2. Data Processing Principles

LeoSync is designed to minimize the transfer of data to external parties.

Photos, videos, metadata and synchronization information are generally processed locally on the Android device.

Files are transferred directly between the Android device and the SMB network share configured by the user.

LeoSync does not use:

- External cloud storage services
- Advertising networks
- Analytics or tracking services
- External synchronization services
- Advertising or analytics SDKs

No usage statistics or personal data are automatically transmitted to the developer.

## 3. Photos and Videos

LeoSync requires access to the photos and videos selected by the user for synchronization.

Files are processed locally on the Android device and transferred directly to the configured SMB network share.

No data is transferred through a cloud service operated by LeoSync or its developer.

Depending on the selected synchronization mode, files may also be transferred from the SMB server to the Android device.

Files are stored and managed at the storage locations selected by the user.

## 4. EXIF Metadata and Location Information

LeoSync can read GPS coordinates embedded in the EXIF metadata of photos.

This information is processed locally and compared against an integrated offline database to determine, for example, the country in which a photo was taken.

No external geolocation, mapping or location services are contacted for this purpose.

GPS coordinates are not automatically transmitted to the developer.

When original photos are transferred, their original EXIF metadata may remain part of the transferred files.

## 5. SMB Connections and Credentials

To connect to an SMB network share, LeoSync requires connection information provided by the user.

This may include:

- Server address
- Share path
- Username
- Password
- Other connection parameters

SMB passwords are stored securely using the Android Keystore.

Credentials are not transmitted to the developer or external cloud services.

File transfers take place directly between the Android device and the configured SMB server.

The security of the connection also depends on the network configuration and security settings of the respective SMB server.

## 6. Diagnostic and Error Reports

LeoSync provides a feature for exporting technical diagnostic reports.

These reports help investigate errors, identify compatibility issues and improve application stability.

Depending on the implementation and the device, a diagnostic report may contain the following information:

### 6.1 Device and Application Information

- Device manufacturer and model
- Android version and SDK version
- Application version and build information
- Manufacturer-specific operating system version

### 6.2 System Status and Permissions

- Battery level and charging status
- Battery optimization status
- Power-saving and background execution status
- Status of relevant application permissions
- Available folder access permissions

### 6.3 Network and Synchronization Information

- Wi-Fi connection status
- Network type and connection status
- Synchronization settings
- Configured synchronization profiles
- Technical status of SMB connections
- Synchronization history and error states

### 6.4 Technical Logs

- Background jobs and WorkManager status
- Warnings and error messages
- Information about terminated processes
- Synchronization statistics
- Timestamps of relevant events

Depending on the contents of the diagnostic report, technical information such as Wi-Fi network names, local server addresses, share paths or file names may also be included.

**SMB passwords are not intentionally included in diagnostic reports.**

### 6.5 Pseudonymous Installation Identifier

Diagnostic reports may contain a randomly generated installation identifier (UUID).

This UUID:

- Is generated locally on the device.
- Is not derived from hardware or device identifiers.
- Does not directly contain personally identifiable information.
- Is used to associate multiple diagnostic reports from the same app installation.

A new installation identifier is generated when the application is reinstalled.

The UUID does not directly identify an individual. However, since it can be used to associate multiple reports, it should be considered a pseudonymous identifier.

## 7. Sending Diagnostic Reports

Diagnostic reports are never transmitted automatically.

The user decides whether to export a report and send it to the developer by email.

The report is sent using the email application or email service selected by the user.

By sending a report, the user voluntarily transmits the technical information contained in it to the developer.

Before sending a report, users should review its contents and be aware that technical details may reveal information about their device and network environment.

The email provider's own privacy policy also applies to the processing of the email.

The developer uses voluntarily submitted diagnostic reports exclusively to investigate technical issues and improve LeoSync.

## 8. Third-Party Services

LeoSync does not use external analytics, advertising, tracking or cloud SDKs.

No user data is automatically shared with advertising networks or analytics providers.

Data may leave the device as part of actions initiated by the user, particularly:

- Synchronization with the configured SMB network share.
- Voluntary submission of diagnostic reports by email.

The Android operating system and email or network services used by the user may independently process additional technical information as part of their own services. Their respective privacy policies apply.

## 9. Data Security

LeoSync uses security mechanisms provided by the Android operating system.

In particular, stored SMB passwords are protected using the Android Keystore.

The application is designed to avoid unnecessary data transfers and to perform data processing locally wherever possible.

The security of file transfers also depends on:

- The security of the local network.
- The configuration of the SMB server.
- The SMB version being used.
- Enabled authentication and encryption mechanisms.

Users should secure their network shares appropriately and avoid exposing SMB services to the public internet without adequate protection.

## 10. Data Storage and Deletion

LeoSync does not operate its own cloud storage for user data.

Files are stored on the Android device and at the SMB storage locations configured by the user.

Local application data and settings can be deleted through Android application management settings. When the application is uninstalled, its associated local application data is removed according to Android system behavior.

Files already transferred to an SMB share are not automatically deleted when LeoSync is uninstalled.

These files must be managed directly on the respective storage system.

Diagnostic reports are initially exported locally. The developer only gains access to their contents when the user voluntarily sends them by email.

## 11. Rights of Data Subjects

Under applicable data protection legislation, individuals may have rights including:

- The right to request access to personal data being processed.
- The right to request correction of inaccurate data.
- The right to request deletion of personal data, where legally applicable.
- The right to object to certain types of data processing.

Since LeoSync does not automatically transmit user data to the developer, the developer generally has no access to users' locally stored photos, videos, settings or synchronization data.

For privacy-related inquiries, please use the contact information provided in Section 1.

Users in Switzerland may also contact the Swiss Federal Data Protection and Information Commissioner (FDPIC).

Website: https://www.edoeb.admin.ch/

## 12. Changes to This Privacy Policy

This privacy policy may be updated if LeoSync's features, technical data processing or applicable legal requirements change.

The latest version will be published on the official Leo project website.

## 13. Contact

For questions regarding the application or data privacy:

**Leo – Independent Software**

Country: Switzerland

Email: [Contact email]