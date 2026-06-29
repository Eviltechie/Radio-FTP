# Radio FTP

This program was written to solve a problem at a radio station where they needed to automatically get files from two different delivery services into their automation system. One delivery system placed files on a FTP server, while the other provided them on a network share. In both situations there was the additional complication of needing to rename the files a specific way for the automation system. The existing commercial options on the market did not prove suitable due to factors involving flexibility, reliability, and the reliance of running in user space.

This program should also work well for automatically grabbing finished recordings off a Blackmagic HyperDeck.

## Features

* Automatically copy or move files from disk or a FTP server.
* Regular expressions are used for matching source files, and substitution is supported for the destination filename.
* A check is made to avoid copying files which are currently being written.
* Files are only recorded as processed if all steps complete.
* Can easily be run as a systemd service on Linux.
* Robust logging.

## How it Works

Each FTP server connection (plus the local filesystem) works in its own thread.

Each thread loops through its list of fetchers. An initial scan of each source directory is made, searching for files which match the associated source regular expression. New files, and files which have changed since last check are noted. A file is considered to be changed if its size or last modified time is different.

After a five second delay, the thread loops through its list of fetchers again. Files are re-checked, and if unchanged since the first check, the file is downloaded to a temporary location. (If the attributes have changed the assumption is the file is still being written and it is not ready to download.) The file is then copied to its final destination, using the associated destination regular expression to perform an optional name substitution. The file name along with the file size and last modified time are then recorded for comparison on next check. Optionally, the source file can be deleted.

The thread then sleeps for a configurable delay before starting the cycle again.

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

## Advanced Configuration

This program uses log4j2. Logs are written to a logs folder in the working directory. Logs are rotated daily, and retained for 14 days. A custom configuration file can be specified with the `log4j2.configurationFile` system property.

Files are written to a temporary directory as an intermediate step. You can manually set the location with the `java.io.tmpdir` system property. This may be handy if you are downloading large files to a network share, and and want the temporary file to be on the same share.

The .db files which are created for each connection are SQLite databases. To clear the list of downloaded files, stop the program and delete the file. An enterprising user may be able to read these files and display them for users on a website...

# Useful Patterns

Here are a couple useful regular expression patterns. Note that backslashes must be escaped in the config file.

## Source Patterns

| Raw | Escaped | Example | Meaning |
|---|---|---|---|
| (?i)(\\d{5})\\.wav$ | (?i)(\\\\d{5})\\\\.wav$ | 12345.wav | Five digits in a capture group followed by .wav. (Ideal for PRX or Content Depot.) |
| (?i).+\\.wav$ | (?i).+\\\\.wav$ | any thing.wav | Matches any .wav file. |
| (?i)775(?:88\|89\|90\|91)\.wav$ | (?i)775(?:88\|89\|90\|91)\\.wav$ | Matches 77588.wav, 77589.wav, 77590.wav, and 77591.wav. |

## Destination Patterns

| Raw | Escaped | Example | Meaning |
|---|---|---|---|
| SH$1.wav | SH$1.wav | SH12345.wav | Prepends SH to the first capture group. |
| $0 | $0 | any thing.wav | Returns the file unchanged. |

## Regular Expression Tips

* If you place an `(?i)` at the start of your expression it will be run case insensitive.
* Placing a `$0` in the destination will return the whole match unchanged, while `$1` will get you the first capture group.
* Use an online tool to help you build and test. Regex101 offers support for Java regular expressions.

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