DROP PROCEDURE IF EXISTS SPLIT;
DROP TABLE IF EXISTS sometbl;

-- In real-world scenario table should have at least PK. And additional indexes in most cases.
-- All these are omitted for demo purposes.

CREATE TABLE sometbl (
  ID   INT,
  NAME VARCHAR(50)
);

INSERT INTO sometbl
VALUES
  (1, 'Smith'),
  (2, 'Julio|Jones|Falcons'),
  (3, 'White|Snow'),
  (4, 'Paint|It|Red'),
  (5, 'Green|Lantern'),
  (6, 'Brown|bag'),
  (7, 'Woods'),
  (8, ''),
  (9, '   '),
  (10, null),
  (11, '|||'),
  (12, ' ||| '),
  (13, ' | | | '),
  (14, ' Abc| ||Def ');

DELIMITER //

CREATE PROCEDURE SPLIT()
  BEGIN

    DECLARE delim CHAR default "|";
    DECLARE v_id INT;
    DECLARE v_name TEXT;
    DECLARE done_loop BOOLEAN DEFAULT FALSE;
    DECLARE items_cursor CURSOR FOR SELECT id, name FROM sometbl;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done_loop = TRUE;

    -- Create temporary table same as original one (and do necessary cleanup)
    DROP TEMPORARY TABLE IF EXISTS temp_table;
    CREATE TEMPORARY TABLE temp_table (
      ID   INT,
      NAME VARCHAR(50)
    ) ENGINE = MEMORY;

    -- Open the cursor and iterate through all the records from the original table
    OPEN items_cursor;

    items_loop: LOOP
      FETCH items_cursor INTO v_id, v_name;
      IF done_loop THEN
        LEAVE items_loop;
      END IF;

      -- Calculate amount of the items within the column
      SET @items_amount = (SELECT LENGTH(v_name) - LENGTH(REPLACE(v_name, delim, '')) + 1);
      SET @i = 1;

      -- Check for string which consist solely of delimiter(s) and(or) space(s)
      SET @not_blank_or_invalid = LENGTH(TRIM(REPLACE(v_name, delim, ''))) != 0;

      -- Just only if it is necessary to split the column
      IF (@not_blank_or_invalid && @items_amount > 1) THEN

        -- Iterate through them
        WHILE @i <= @items_amount DO

        -- Find the next value (a good candidate for separate function, but not this time for brevity purposes)
        SET @split_value =
          (SELECT
             REPLACE(
                 SUBSTRING(
                     SUBSTRING_INDEX(v_name, delim, @i),
                     LENGTH(SUBSTRING_INDEX(v_name, delim, @i - 1)) + 1
                 ), delim, ''
             )
          );

          -- Insert it into in-memory temporary table skipping empty and space-only values
          IF (LENGTH(TRIM(@split_value)) > 0) THEN
            INSERT INTO temp_table VALUES (v_id, @split_value);
          END IF;

          SET @i = @i +1;

        END WHILE;

        -- Drop the currently processed row from original table
        DELETE FROM sometbl WHERE id = v_id;
        -- Insert all rows from temporary table
        -- The task doesn't specify target table for split columns. Assuming to use the same table.
        -- Since we have no PKs here, no issues will be with inserting in the same table.
        -- Otherwise it is necessary to avoid inserting duplicate records.
        INSERT INTO sometbl SELECT * FROM temp_table WHERE id = v_id;
        -- And clean it up
        DELETE FROM temp_table;

      END IF;

    END LOOP items_loop;

    CLOSE items_cursor;

  END//

DELIMITER ;

CALL SPLIT();
SELECT * from sometbl;

DROP PROCEDURE SPLIT;
DROP TABLE sometbl;