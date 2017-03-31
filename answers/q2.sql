DROP FUNCTION IF EXISTS INITCAP;

-- Algorithm complexity: O(n) where n is string length. More precisely, I'd say, it should be O(2n),
-- since before processing the input string is being lowercased (SET input_string := LOWER(input_string))
-- which is also O(n) I suspect (despite the fact I didn't see the real implementation of LOWER() function).
-- If so, the entire INITCAP() function complexity is O(n).

-- One of the other implementation was to upper/lower case each char separately instead of lowercasing
-- the entire sting and uppercacing only necessary chars. This was a bit less performant according to
-- BENCHMARK() statistics.

DELIMITER //
CREATE FUNCTION INITCAP(
  input_string TEXT CHARSET utf8
)
  RETURNS TEXT CHARSET utf8
  BEGIN
    DECLARE chr TEXT CHARSET utf8;
    DECLARE result TEXT CHARSET utf8 DEFAULT '';
    DECLARE input_length INT;
    DECLARE char_idx INT DEFAULT 1;
    DECLARE capitalize BOOLEAN DEFAULT TRUE; -- First char should be capitalized

    SET input_length := CHAR_LENGTH(input_string);
    -- First, lowercase the entire input string
    SET input_string := LOWER(input_string);

    -- Next, check the string char by char
    WHILE (char_idx <= input_length) DO

      -- Take next char from the input string
      SET chr := MID(input_string, char_idx, 1);

      -- Assuming word boundary is a space. In real life set of punctuation chars should be a bit wider: ,.-?!:;()[]"'/

      -- If this is word boundary (a space)...
      IF (chr = ' ') THEN
        -- ... next char should be capitalized
        SET capitalize := TRUE;
      ELSE
        -- If not and previous char was word boundary...
        IF (capitalize) THEN
          -- ...capitalize this char
          SET chr := UPPER(chr);
          SET capitalize := FALSE;
        END IF;
      END IF;

      -- Add current character to the result string
      SET result := CONCAT(result, chr);

      SET char_idx := char_idx + 1;
    END WHILE;

    RETURN result;
  END//

DELIMITER ;

SELECT INITCAP("a  UNITED states Of AmERIca СОЕДИНЕННЫЕ штаты АмЕРИКи СПОЛУЧЕНI штати АмЕРИки ї є");
SELECT INITCAP("   UNITED states Of AmERIca СОЕДИНЕННЫЕ штаты АмЕРИКи СПОЛУЧЕНI штати АмЕРИки ї є");
SELECT INITCAP(null);

DROP FUNCTION INITCAP;