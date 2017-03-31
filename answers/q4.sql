DROP TABLE IF EXISTS bugs;

-- For demo purposes DATE type is used. Usually, for globally accessible applications it is necessary to
-- keep the timezone information and convert dates between timezones, so TIMESTAMP type would be more suitable.
-- If there's a high volume of data is planned to be kept in this table, TIMESTAMP will create more overhead
-- (will take more storage space) and if this app is used within the office/home(s) in the same timezone,
-- simple DATE is preferred.

CREATE TABLE bugs (
  ID         INT  AUTO_INCREMENT,
  OPEN_DATE  DATE NOT NULL,
  CLOSE_DATE DATE DEFAULT NULL,
  SEVERITY   INT  NOT NULL DEFAULT 0,
  PRIMARY KEY PK (ID),
  INDEX OD (OPEN_DATE),
  INDEX CD (CLOSE_DATE)
);

INSERT INTO bugs (OPEN_DATE, CLOSE_DATE)
    VALUES
      ('2012-01-01', null),
      ('2012-01-01', '2012-03-31'),
      ('2012-01-01', '2012-02-29'),
      ('2012-01-01', '2012-03-06'),
      ('2012-01-01', '2012-03-13'),

      ('2012-02-05', '2012-02-20'),
      ('2012-02-07', '2012-03-14'),
      ('2012-02-08', '2012-02-23'),
      ('2012-02-08', null),

      ('2012-03-04', '2012-03-20'),
      ('2012-03-07', '2012-03-14'),
      ('2012-03-08', '2012-03-23'),
      ('2012-03-08', null),

      ('2012-03-01', '2012-03-21');

SET @from='2012-02-01';
SET @to='2012-04-01';

SELECT dates.date,
  (SELECT count(*) FROM bugs WHERE OPEN_DATE <=dates.date AND (CLOSE_DATE IS NULL OR CLOSE_DATE >dates.date)) as open_bugs
FROM (
  -- This is a cheapest way to generate range of dates, but this implementation has it's limitation: date range can't be
  -- more than 1000 days (2.73 years) from "TO" date. But it is extendable up to 10000 (27.4 years) by creating additional
  -- cross join with "SELECT ... UNION ALL" like three of these and including it into "INTERVAL ... DAY" . So, actually
  -- necessary dates range may define the size of the tricky part of this query (which may be a bit shorter than 10*10*10).
       SELECT DATE(@to) - INTERVAL (a.a + (10 * b.a) + (100 * c.a)) DAY AS date
       FROM
         (SELECT 0 AS a UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) AS a
         CROSS JOIN
         (SELECT 0 AS a UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) AS b
         CROSS JOIN
         (SELECT 0 AS a UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) AS c
     ) dates

WHERE dates.date BETWEEN @from AND @to
ORDER BY dates.date;

DROP TABLE bugs;
