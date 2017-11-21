/*
 * DDL and maybe some queries for the blockchain data put into S3 storage by Gemfire.
 * The data is JSON formatted.
 *
 * 13 November 2017
 */

-- Prep. the database
CREATE OR REPLACE FUNCTION write_to_s3() RETURNS integer AS
   '$libdir/gps3ext.so', 's3_export' LANGUAGE C STABLE;

CREATE OR REPLACE FUNCTION read_from_s3() RETURNS integer AS
   '$libdir/gps3ext.so', 's3_import' LANGUAGE C STABLE;

CREATE PROTOCOL s3 (writefunc = write_to_s3, readfunc = read_from_s3);

/*
 * Basically, proceed with the steps outlined here:
 * https://gpdb.docs.pivotal.io/500/admin_guide/external/g-s3-protocol.html
 *
 * NOTE: the table definition below assumes /home/gpadmin/s3.conf is the location of the S3
 * configuration file, on each of the hosts, where s3.conf has the following format:
 *
[default]
secret = "SOME_KEY_WITH_LENGTH_40_CHARS"
accessid = "SOME_ID_WITH_LENGTH_20_CHARS"
threadnum = 3
chunksize = 67108864
 *
 */

DROP EXTERNAL TABLE IF EXISTS blockchain_txn_s3;
CREATE EXTERNAL TABLE blockchain_txn_s3
(
  txn JSON
)
/* All the data: */
-- LOCATION('s3://s3-us-west-2.amazonaws.com/io.pivotal.dil.blockchain/BlockchainTxn config=/home/gpadmin/s3.conf')
/* Only the data for November 14, 2017: */
--LOCATION('s3://s3-us-west-2.amazonaws.com/io.pivotal.dil.blockchain/BlockchainTxn/20171114 config=/home/gpadmin/s3.conf')
--LOCATION('s3://s3-us-west-2.amazonaws.com/io.pivotal.dil.blockchain/BlockchainTxn/201711 config=/home/gpadmin/s3.conf')
--LOCATION('s3://s3-us-west-2.amazonaws.com/io.pivotal.reinvent.demo/BlockchainTxn/201711 config=/home/gpadmin/s3.conf')
--LOCATION('s3://s3-us-west-2.amazonaws.com/io.pivotal.reinvent.demo/BlockchainTxn/20171120-19 config=/home/gpadmin/s3.conf')
LOCATION('s3://s3-us-west-2.amazonaws.com/io.pivotal.reinvent.demo/BlockchainTxn/20171121 config=/home/gpadmin/s3.conf')
FORMAT 'TEXT' (DELIMITER 'OFF' NULL '\N' ESCAPE '\');

-- TODO: Use the above approach to build up an external partition setup

-- Below are some query examples based on the JSON capability

-- Convert the date to a timestamp with time zone
SELECT (txn->>'time_as_date')::TIMESTAMP WITH TIME ZONE FROM blockchain_txn_s3 LIMIT 10;

-- Try to get to the elements of the arrays
-- See https://stackoverflow.com/questions/22736742/query-for-array-elements-inside-json-type
SELECT txn->>'hash', JSON_ARRAY_ELEMENTS(txn->'inputs') FROM blockchain_txn_s3 LIMIT 5;

-- Heap table for BlockchainTxn data
DROP TABLE IF EXISTS blockchain_txn;
CREATE TABLE blockchain_txn
(
  hash TEXT -- This is the PK
  , lock_time INT -- 494377
  , relayed_by TEXT -- 0.0.0.0
  , size INT -- 373
  , time TIMESTAMP WITH TIME ZONE -- to_timestamp(1510695818)::timestamp with time zone
  , tx_index BIGINT -- 301261089
  , ver INT -- 2
  , vin_sz INT -- 2
  , vout_sz INT -- 2
)
WITH (APPENDONLY=true, COMPRESSTYPE=ZLIB)
DISTRIBUTED BY (hash);

-- Heap table for BlockchainItem data
DROP TABLE IF EXISTS blockchain_item;
CREATE TABLE blockchain_item
(
  id TEXT -- This is synthetic: addr || '-' || tx_index (see the BlockchainItem class)
  , hash TEXT -- This is the FK to the parent BlockchainTxn
  , direction TEXT
  , spent BOOLEAN
  , tx_index BIGINT
  , type INT
  , addr TEXT
  , value BIGINT
  , n INT
  , script TEXT
)
WITH (APPENDONLY=true, COMPRESSTYPE=ZLIB)
DISTRIBUTED BY (hash);

-- Load the data
INSERT INTO blockchain_txn
SELECT
  (txn->>'hash')::TEXT
  , (txn->>'lock_time')::INT
  , (txn->>'relayed_by')::TEXT
  , (txn->>'size')::INT
  , TO_TIMESTAMP((txn->>'time')::INT)::TIMESTAMP WITH TIME ZONE
  , (txn->>'tx_index')::BIGINT
  , (txn->>'ver')::INT
  , (txn->>'vin_sz')::INT
  , (txn->>'vout_sz')::INT
FROM blockchain_txn_s3;
ANALYZE blockchain_txn;

-- From Alastair Turner
INSERT INTO blockchain_item
WITH inputs AS (
  SELECT txn->>'hash' AS hash, 'in'::varchar AS direction, json_array_elements(json_extract_path(txn, 'inputs'))->'prev_out' AS item_body
    FROM blockchain_txn_s3
),
outputs AS (
  SELECT txn->>'hash' AS hash, 'out'::varchar AS direction, json_array_elements(json_extract_path(txn, 'out')) AS item_body
    FROM blockchain_txn_s3
),
all_items AS (
    SELECT * FROM inputs
  UNION ALL
    SELECT * FROM outputs
)
SELECT
  -- NOTE: this would be the DDL for the blockchain_item table
  (item_body->>'id')::TEXT id -- This is synthetic: addr || '-' || tx_index (see the BlockchainItem class)
  , hash::TEXT -- This is the FK to the parent BlockchainTxn
  , direction
  , (item_body->>'spent')::BOOLEAN spent
  , (item_body->>'tx_index')::BIGINT tx_index
  , (item_body->>'type')::INT type
  , (item_body->>'addr')::TEXT addr
  , (item_body->>'value')::BIGINT value
  , (item_body->>'n')::INT n
  , (item_body->>'script')::TEXT script
FROM all_items;
ANALYZE blockchain_item;

-- Get a histogram of amount of coin transacted by day of week
SELECT DATE_PART('dow', t.time) dow, SUM(i.value / 100000000.0)::NUMERIC(20, 2) sum
FROM blockchain_txn t, blockchain_item i
WHERE t.hash = i.hash
GROUP BY dow
ORDER BY dow ASC;

-- Get a histogram of amount of coin transacted by day of week, but naming the days
SELECT
  (ARRAY['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'])[DATE_PART('dow', t.time) + 1] dow,
  SUM(i.value / 100000000.0)::NUMERIC(20, 2) sum
FROM blockchain_txn t, blockchain_item i
WHERE t.hash = i.hash
GROUP BY dow
ORDER BY dow ASC;

-- Get a histogram of amount of coin transacted by hour of the day (in the US East time zone)
-- Units of bitcoin are THOUSANDS.  This is intended to feed a d3.js bar graph display
SELECT LPAD(DATE_PART('hour', t.time)::TEXT, 2, '0') || ':00' hour_of_day, (((SUM(i.value / 100000000.0))/1000.0)::BIGINT) sum
FROM blockchain_txn t, blockchain_item i
WHERE t.hash = i.hash
GROUP BY hour_of_day
ORDER BY hour_of_day ASC;

-- Check for duplicate transactions?
WITH x AS (
  SELECT hash, COUNT(*) cnt FROM blockchain_txn
  GROUP BY 1 ORDER BY 2
)
SELECT COUNT(*) FROM x WHERE x.cnt > 1;

-- Query the S3 external table directly, to verify the most recent data in S3
SELECT
  (txn->>'hash')::TEXT
  , (txn->>'lock_time')::INT
  , (txn->>'relayed_by')::TEXT
  , (txn->>'size')::INT
  , TO_TIMESTAMP((txn->>'time')::INT)::TIMESTAMP WITH TIME ZONE txn_date
  , (txn->>'tx_index')::BIGINT
  , (txn->>'ver')::INT
  , (txn->>'vin_sz')::INT
  , (txn->>'vout_sz')::INT
FROM blockchain_txn_s3
ORDER BY txn_date DESC
LIMIT 10;


