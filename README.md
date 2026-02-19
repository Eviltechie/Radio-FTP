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