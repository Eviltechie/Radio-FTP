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
| $& | $& | any thing.wav | Returns the file unchanged. |

# FTPS Nonsense

* https://stackoverflow.com/questions/32398754/how-to-connect-to-ftps-server-with-data-connection-using-same-tls-session
* https://issues.apache.org/jira/browse/NET-408
* https://stackoverflow.com/questions/61348963/android-ftps-session-reuse-no-field-sessionhostportcache
* https://stackoverflow.com/questions/70903926/how-to-establish-a-ftps-data-connection-to-a-filezilla-server-1-2-0
* https://github.com/iterate-ch/cyberduck/blob/master/ftp/src/main/java/ch/cyberduck/core/ftp/FTPClient.java
