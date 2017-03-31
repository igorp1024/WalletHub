DROP TABLE IF EXISTS votes;

-- In real-world scenario table should hav at least PK. And additional indexes in most cases.
-- All these are omitted for query demo purposes.

CREATE TABLE votes (
  name  CHAR(10),
  votes INT
);
INSERT INTO votes VALUES
  ('Smith', 10), ('Jones', 15), ('White', 20), ('Black', 40), ('Green', 50), ('Brown', 20);

-- Session variable is used as a counter, so rank calculation complexity is O(1),
-- as it is always just an increment
SELECT
  @r := @r + 1 AS rank,
  votes.*
FROM votes, (SELECT @r := 0) AS r
ORDER BY votes.votes, votes.name; -- Order by name for those records which have the same votes

DROP TABLE IF EXISTS votes;