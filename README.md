# Useful Patterns

## Source Patterns

| Raw | Escaped | Example | Meaning |
|---|---|---|---|
| (\d{5})\.wav$ | (\\d{5})\\.wav$ | 12345.wav | Five digits in a capture group followed by .wav |
| .+\.wav$ | .+\\.wav$ | any thing.wav | Matches any .wav file. |

## Destination Patterns

| Raw | Escaped | Example | Meaning |
|---|---|---|---|
| SH$1.wav | SH$1.wav | SH12345.wav | Prepends SH to the first capture group. |
| $0 | $0 | any thing.wav | Returns the file unchanged. |

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