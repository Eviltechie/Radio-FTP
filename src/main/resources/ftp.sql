--
-- File generated with SQLiteStudio v3.4.21 on Fri Feb 20 00:51:24 2026
--
-- Text encoding used: System
--
PRAGMA foreign_keys = off;
BEGIN TRANSACTION;

-- Table: files
CREATE TABLE IF NOT EXISTS files (host TEXT, fetcher TEXT, file TEXT, size INTEGER, modified TEXT, PRIMARY KEY (host, fetcher, file));

COMMIT TRANSACTION;
PRAGMA foreign_keys = on;
