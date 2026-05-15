# Configuration

The configuration must be named `config.json` and be in your working directory. If the file does not exist, a sample will be created for you on first run, and then the program will exit.

## FTP Configuration

The `ftpHosts` is an array of FTP hosts that you would like to connect to, which can be empty if you have none.

* The options for `host`, `port`, `ftps`, `username`, and `password` should be self explanatory.
* For `scanDelay`, this is the time in seconds that you wish to wait between each scan to check for new files.

## Local Configuration

The `scanDelay` for local file copy works identically compared to FTP copy. If you have no local copy jobs, leave `fetchers` empty to disable.

## Common

For both FTP and local, the `fetchers` is an array of "fetchers", which can loosely be thought about as "jobs", each containing a source path, source pattern, destination path, and destination pattern.

* `name` Uniquely identifies the fetcher/job in case more than one has the same source folder.
* `action` - Can be `copy` or `move`. Setting to `copy` will copy the file only, while setting to `move` will delete the source file after it has been written to the destination directory.
* `wetRun` - Will mark files as processed, but will not actually download/copy them. **CAUTION:** Setting wet run with the move option will delete source files. This option is provided to allow for the program to "catch up" without having to spend time/bandwidth downloading files in case of migration from another system or when rebuilding after a catastrophic failure.
* `sourcePath` - Path to a folder to search for files. If running on Windows, you can use forward slashes to avoid nonsense with escaping backslashes.
* `sourcePattern` - Java regular expression. Files matching this pattern will be processed. Capture groups are supported for use in the `destinationPattern`.
* `destinationPath` - Path to place files after downloading. If running on Windows, you can use forward slashes to avoid nonsense with escaping backslashes.
* `destinationPattern` - Java regular expression pattern for the destination filename. If you used capture groups in the source pattern you can then use those groups here.

# Useful Patterns

## Source Patterns

| Raw | Escaped | Example | Meaning |
|---|---|---|---|
| (?i)(\\d{5})\\.wav$ | (?i)(\\\\d{5})\\\\.wav$ | 12345.wav | Five digits in a capture group followed by .wav. (Ideal for PRX or Content Depot.) |
| (?i).+\\.wav$ | (?i).+\\\\.wav$ | any thing.wav | Matches any .wav file. |

## Destination Patterns

| Raw | Escaped | Example | Meaning |
|---|---|---|---|
| SH$1.wav | SH$1.wav | SH12345.wav | Prepends SH to the first capture group. |
| $0 | $0 | any thing.wav | Returns the file unchanged. |

## Useful Tips

* If you place an `(?i)` at the start of your expression it will be run case insensitive.
* Placing a `$0` in the destination will return the whole match unchanged, while `$1` will get you the first capture group.

# systemd service file

A sample systemd service file is provided in this repository. Just adjust the username and jar file to suit.

# FTPS Nonsense

FTPS should work, but it uses reflection due to the NET-408 issue with Apache Commons Net. It's possible it may break unexpectedly at a future date. Additionally it only supports TLS 1.2, not TLS 1.3. Regular FTP should work without issue.

* https://stackoverflow.com/questions/32398754/how-to-connect-to-ftps-server-with-data-connection-using-same-tls-session
* https://issues.apache.org/jira/browse/NET-408
* https://stackoverflow.com/questions/61348963/android-ftps-session-reuse-no-field-sessionhostportcache
* https://stackoverflow.com/questions/70903926/how-to-establish-a-ftps-data-connection-to-a-filezilla-server-1-2-0
* https://github.com/iterate-ch/cyberduck/blob/master/ftp/src/main/java/ch/cyberduck/core/ftp/FTPClient.java

Currently working on the following Java version:

* openjdk version "25.0.2" 2026-01-20 LTS
* OpenJDK Runtime Environment Temurin-25.0.2+10 (build 25.0.2+10-LTS)
* OpenJDK 64-Bit Server VM Temurin-25.0.2+10 (build 25.0.2+10-LTS, mixed mode, sharing)


100% Free of AI