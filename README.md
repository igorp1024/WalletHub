##MySQL test

1) Write a query to rank order the following table in MySQL by votes, display the rank as one of the
columns.
    ```sql
    CREATE TABLE votes ( name CHAR(10), votes INT );
    INSERT INTO votes VALUES ('Smith',10), ('Jones',15), ('White',20), ('Black',40), ('Green',50), ('Brown',20);
    ```

1) Write a function to capitalize the first letter of a word in a given string;
Example: 
    ```sql 
    initcap(UNITED states Of AmERIca ) = United States Of America
    ```

1) Write a procedure in MySQL to split a column into rows using a delimiter.
    ```sql
    CREATE TABLE sometbl ( ID INT, NAME VARCHAR(50) );
    INSERT INTO sometbl 
          VALUES
              (1, 'Smith'),
              (2, 'Julio|Jones|Falcons'),
              (3, 'White|Snow'),
              (4, 'Paint|It|Red'),
              (5, 'Green|Lantern'),
              (6, 'Brown|bag');
    ```
    For (2), example rows would look like >> `3, white`, `3, Snow` `...`

1) I have a table for bugs from a bug tracking software; let’s call the table `bugs`.
The table has four columns `(id, open_date, close_date, severity)`. On any given day
a bug is open if the open_date is on or before that day and close_date is after
that day. For example, a bug is open on `“2012-01-01”`, if it’s created on or
before `“2012-01-01”` and closed on or after `“2012-01-02”`. I want an SQL to show
the number of bugs open each day for a range of dates. _Hint: There are bugs that were
never closed._

##JAVA test
1) Write an efficient algorithm to check if a string is a palindrome. A string is a
palindrome if the string matches the reverse of string.
Example: `1221` is a palindrome but not `1121`.

1) Write an efficient algorithm to find K-complementary pairs in a given array of
integers. Given Array `A`, pair `(i, j)` is K-complementary if `K = A[i] + A[j]`;

1) Given a large file that does not fit in memory (say 10GB), find the top 100000
most frequent phrases. The file has 50 phrases per line separated by a pipe (`|`).
Assume that the phrases do not contain pipe.
Example line may look like: 
```Foobar Candy | Olympics 2012 | PGA | CNET | Microsoft Bing ....```
The above line has 5 phrases in visible region.