=======================================
 SQL test
=======================================

Task #1
-------
Nothing to add except what is mentioned in q1.txt.

Task #2
-------
Nothing to add except what is mentioned in q2.txt.

Task #3
-------
Nothing to add except what is mentioned in q3.txt.

Task #4
-------
Nothing to add except what is mentioned in q4.txt.

=======================================
 JAVA test
=======================================

General notes.

1. One of the requirements of the technical task is to keep the implementation within
one file. Just to not complicate things more than necessary, each java task is implemented as
set of static methods. In real life, some of them (task #3) should be designed as set of more
testable components (classes).

2. Tests infrastructure is rather demonstrable than realistic. Real unit tests should also test
inner components structure. The implemented ones are for testing source data processing.

3. A general java file structure:
    +================+
    | Implementation |
    +================+
    | Implementation |
    | auxiliary code |
    +================+
    |     main()     |
    +================+
    |  Convenience   |
    |  and utility   |
    |  test methods  |
    +================+

4. Convenience test methods are not thoroughly documented in javadoc, just short explanation of
their purpose is given.


Task #1.
--------
Nothing to add.

Task #2.
--------
Nothing to add.

Task #3.
--------
There may be several ways to solve the problem.

1. Most naïve solution. First, use a map, put there each phrase as key and 1 as decimal value.
Increment value each time the phrase is found. Second, walk through the map values and find the
top 100000. Without detailed examination of second part, the first one is violating the primary
constraint. In worst case all phrases are unique and this map won't fit in memory.

2. One of the ways of solving this task is "trie and min heap". This method require building trees.
In worst case, when 10 GB file contains 10GB length single phrase and a 49 of single-char phrases,
at least trie won't fit in memory.

3. This task looks to be a perfect fit for Hadoop. Nevertheless, I will not implement it due to
lack of experience working with Hadoop framework.

4. My implementation (if I'm not mistaken) may be quite close to Hadoop's one or even reinvention
of it. Primary goal is to be able to process the data, which may be even more than 10GB (in any case
it won't fit in memory). So, have to neglect processing speed efficiency in favour of memory
efficient implementation.