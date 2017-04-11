import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 * Top phrases search implementation.
 *
 * NOTE: this class requires JUnit library (http://junit.org). Tested on version 4.12.
 */
public class TopPhrases {

    public static final String MESSAGE_DIGEST_ALGORITHM = "SHA-256";
    public static char SEPARATOR = '|';

    private static Set<Path> storages = new HashSet<>();
    private static boolean keepStorages = false;

    static {
        // Cleanup on JVM shutdown (remove storages left by search code)
        Runtime.getRuntime().addShutdownHook(
            new Thread(
                () -> {
                    if (!keepStorages) {
                        storages.forEach(storagePath -> {
                                             try {
                                                 if (Files.exists(storagePath)) {
                                                     StorageUtils.dropStorage(storagePath);
                                                 }
                                             } catch (IOException ioe) {
                                                 throw new RuntimeException(
                                                     String.format(
                                                         "Can't delete dir: \"%s\"",
                                                         storagePath.toFile().getAbsolutePath()),
                                                     ioe);
                                             }
                                         }
                        );
                    }
                }
            )
        );
    }

    /**
     * Finds <tt>topLimit</tt> most frequent phrases in the <tt>sourceFilePath</tt> text file.
     * Phrases should be separated with {@link #SEPARATOR}.
     *
     * @param sourceFilePath a <tt>String</tt> path to the file to analyze.
     * @param topLimit       amount of top phrases.
     * @return a {@link TreeSet<Phrase>} containing selected data.
     */
    public static TreeSet<Phrase> findTopPhrases(String sourceFilePath, int topLimit) {

        final Path storagePath = getStoragePath();

        scheduleForCleanupOnJvmShutdown(storagePath);

        // And delete it before the search process has started (this is highly unlikely)
        cleanupBeforehandIfNecessary(storagePath);

        // Map phrases to their count numbers. For this method memory use is O(1), because reading
        // is performed into buffer of the fixed size, and filesystem use is O(n), where n is
        // phrases amount. Code complexity: O(n), where n is phrases amount.
        map(storagePath, sourceFilePath);

        // Reduce the set of phrases to the limit.
        // Memory use is O(n) (since top phrases are kept in TreeMap), where n is phrases amount.
        // Algorithm complexity: search on file system is O(n), inserting into TreeMap is O(n*log n)
        // So, totally is O(n*log n) for this method.
        return reduce(storagePath, topLimit);

        // Ultimately, memory use: O(1 + n) = O(n), algorithm complexity: O(n + n + n*log n) = O(n*log n),
        // where n is phrases amount.
        // But filesystem I/O overhead is also should be taken into consideration.
    }

    /**
     * Returns a base storage path containing mapped data. Base directory is bound to the current
     * thread which allows each thread to have it's own storage if necessary.
     *
     * @return a {@link Path} referencing the base storage directory.
     */
    public static Path getStoragePath() {
        // A separate storage for each thread
        return Paths.get("out/storage_" + System.identityHashCode(Thread.currentThread()));
    }

    /**
     * Schedules the storage base directory for recursive deletion on JVM shutdown.
     *
     * @param storagePath a {@link Path} referencing the base storage directory.
     */
    private static synchronized void scheduleForCleanupOnJvmShutdown(Path storagePath) {
        storages.add(storagePath);
    }

    /**
     * Removes the directory if it exists and shows a warning if so.
     *
     * @param storagePath a {@link Path} to check and delete.
     */
    private static void cleanupBeforehandIfNecessary(Path storagePath) {

        try {
            // Cleanup the storage if it was left since last shutdown
            if (Files.exists(storagePath)) {
                System.out.println("Unclean storage found. Fixing that...");
                StorageUtils.dropStorage(storagePath);
            }

        } catch (IOException ioe) {
            throw new RuntimeException(
                String.format(
                    "Can't delete the storage dir: \"%s\". Exiting",
                    storagePath.toFile().getAbsolutePath()),
                ioe);
        }
    }

    /**
     * Maps phrases to phrase count within the file specified in <tt>sourcePath</tt>.
     *
     * @param storagePath    a {@link Path} to the storage directory where mapped data is kept.
     * @param sourceFilePath a <tt>String</tt> path to the file with phrases.
     */
    private static void map(Path storagePath, String sourceFilePath) {

        try {
            final int BUF_SIZE = 1024 * 8;
            MessageDigest md = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM);

            try (FileInputStream fis = new FileInputStream(sourceFilePath)) {
                byte[] buf = new byte[BUF_SIZE];
                int bytesRead;

                File tempFile = new File("temp_phrase_container");
                FileOutputStream fos = new FileOutputStream(tempFile);
                do {

                    bytesRead = fis.read(buf, 0, BUF_SIZE);
                    int startPos = 0;

                    // Look for phrase boundary chars
                    for (int i = 0; i < bytesRead; i++) {

                        boolean lineBoundaryReached = buf[i] == '\n' || buf[i] == '\r';
                        boolean phraseBoundaryReached = buf[i] == SEPARATOR;

                        if (lineBoundaryReached || phraseBoundaryReached) {

                            // This is phrase boundary. Write last chunk to the file.
                            fos.write(buf, startPos, i - startPos);
                            // Update the digest
                            md.update(buf, startPos, i - startPos);
                            // Don't forget to close it
                            fos.close();
                            // Register the phrase container file (increase phrase counter)
                            StorageUtils.registerDigest(md.digest(), storagePath, tempFile);
                            // Resetting the digest engine
                            md.reset();
                            // Reinitialize temp file
                            fos = new FileOutputStream(tempFile);

                            // Alter starting position in buffer;
                            startPos = i + 1;
                        }
                    }

                    // Store the rest of the buffer
                    if (startPos != bytesRead) {
                        fos.write(buf, startPos, bytesRead - startPos);
                        md.update(buf, startPos, bytesRead - startPos);
                    } else {
                        // If at EOF and buffer is completely processed, remove empty file
                        if (fis.available() == 0) {
                            fos.close();
                            tempFile.delete();
                        }
                    }

                } while (bytesRead == BUF_SIZE && fis.available() > 0);
            }

        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("Can't map phrases", e);
        }
    }

    /**
     * Reduces the list of top phrases to the <tt>topLimit</tt> value.
     *
     * @param storagePath a {@link Path} to the storage directory where mapped data is kept.
     * @param topLimit    necessary amount of top phrases.
     * @return a {@link TreeSet<Phrase>} containing top <tt>topLimit</tt> phrase descriptors.
     */
    private static TreeSet<Phrase> reduce(Path storagePath, int topLimit) {

        TreeSet<Phrase> topPhrases = new TreeSet<>();

        try {
            Files.walk(storagePath)
                // Skip directories
                .filter(entry -> !Files.isDirectory(entry))
                // Build relative path without storage path and filename (counter)
                .forEach(
                    entry -> {

                        // Add next item
                        int nameCount = entry.getNameCount();
                        topPhrases.add(
                            new Phrase(
                                // Match count
                                Integer.valueOf(entry.subpath(nameCount - 1, nameCount).toString()),
                                // Digest value with file separator chars
                                entry.subpath(storagePath.getNameCount(), nameCount - 1).toString())
                        );

                        // Keep a topLimit amount of phrases
                        if (topPhrases.size() > topLimit) {
                            topPhrases.remove(topPhrases.last());
                        }
                    }
                );
        } catch (IOException e) {
            throw new RuntimeException("Can't count top phrases", e);
        }

        return topPhrases;
    }

    private static class Phrase implements Comparable<Phrase> {

        private int count;
        private String relativePath;

        public Phrase(int count, String relativePath) {
            this.count = count;
            this.relativePath = relativePath;
        }

        @Override
        public int compareTo(Phrase p) {

            if (p == null) {
                return 1;
            }

            // For phrases with same count compare their hashes to avoid treating them as duplicates
            if (count == p.count) {
                return relativePath.compareTo(p.relativePath);
            }

            return p.count - count; // Reverse order (from greater to lesser)
        }
    }

    /**
     * Convenience class for storage manipulation.
     */
    private static class StorageUtils {

        /**
         * Registers digest in internal storage and converts it to special directory name (see
         * {@link #digestToDirectoryStructure(byte[])} for details ). Then stores the
         * <tt>phraseFile</tt> in that directory under "1" filename (if that directory didn't exist)
         * or treats filename as phrase count value, increases it and renames the file giving new
         * count value as new name.
         *
         * @param digest      a digest to register.
         * @param storagePath a {@link Path} to the storage directory where mapped data is kept.
         * @param phraseFile  the <tt>File</tt> containing a phrase.
         */
        public static void registerDigest(byte[] digest, Path storagePath, File phraseFile) {

            // Convert digest to string (a directories hierarchy which contain the file)
            String dirHierarchy = digestToDirectoryStructure(digest);

            // Look for the directory to be present
            File hashDirFile = new File(storagePath.toFile(), dirHierarchy);

            if (hashDirFile.exists()) {

                // The directory exists, there should be a file containing the phrase and with
                // integer count as it's name (how man y times it is present in the document)
                File hashFile = hashDirFile.listFiles()[0];

                // There's a file already, but it has different size than the passed one
                // This means that it is a collision in digest algorithm (same digest value for two
                // different files)
                if (hashFile.length() != phraseFile.length()) {
                    throw new IllegalStateException(
                        String.format(
                            "Digest algorithm collision detected for %s and %s",
                            hashDirFile.getAbsolutePath(),
                            phraseFile.getAbsolutePath()));
                }
                // Increase count for current phrase (rename file,
                // for instance from "12344" to "12345").
                Integer newCount = Integer.valueOf(hashFile.getName()) + 1;
                hashFile.renameTo(new File(hashDirFile, newCount.toString()));

            } else {

                // The directory hierarchy doesn't exist, create it
                hashDirFile.mkdirs();
                // Initialize "counter"
                phraseFile.renameTo(new File(hashDirFile, "1"));
            }
        }

        private static final int PART_SIZE = 3; // 16^3 = max 4096 directories within a directory

        /**
         * Converts <tt>byte[]</tt> digest to hex string split in parts of <tt>PART_SIZE</tt>, which
         * is treated as a directories hierarchy.
         *
         * For instance, "abcdefghij" with PART_SIZE=3 will produce the following hierarchy:
         * <pre>
         * abc
         *  |
         *  +-def
         *     |
         *     +-ghi
         *        |
         *        +-j
         * </pre>
         * This is necessary due to some filesystems limitation (there are limits for 32767 files
         * per directory on some filesystems, like ext3).
         *
         * @param digest a <tt>byte[]</tt> containing digest.
         * @return a directories hierarchy path as string.
         */
        private static String digestToDirectoryStructure(byte[] digest) {

            String digestHexStringValue = toHexString(digest);

            // Split the directory name in parts by PART_SIZE chars
            int length = digestHexStringValue.length();

            StringBuilder sb = new StringBuilder(length + length / PART_SIZE);
            for (int i = 0; i < digestHexStringValue.length(); i++) {
                if (i % PART_SIZE == 0) {
                    sb.append(File.separator);
                }
                sb.append(digestHexStringValue.charAt(i));
            }

            return sb.toString();
        }

        private final static char[] alphabet = "0123456789ABCDEF".toCharArray();

        /**
         * Converts byte array to it's hex string representation.
         *
         * @param byteArray an array to convert
         * @return hex string representation of array bytes.
         */
        private static String toHexString(byte[] byteArray) {
            char[] hexChars = new char[byteArray.length * 2];
            for (int i = 0; i < byteArray.length; i++) {
                int v = byteArray[i] & 0xFF;
                hexChars[i * 2] = alphabet[v >>> 4];
                hexChars[i * 2 + 1] = alphabet[v & 0x0F];
            }
            return new String(hexChars);
        }

        /**
         * Recursively deletes storage files and dirs.
         *
         * @throws IOException on any I/O issue when deleting directory content.
         */
        private static void dropStorage(Path storagePath) throws IOException {
            System.out.println("Removing the storage directory");

            Files.walk(storagePath)
                .map(Path::toFile)
                .sorted((o1, o2) -> -o1.compareTo(o2))
                .forEach(File::delete);
        }
    }

    // Just for the tests brevity those exceptions which may happen during tests preparations
    // are not handled. So, the main() method just "throws Exception".
    public static void main(String[] args) throws Exception {

        // First, small trick to keep storages in case JUnit assertion terminates the execution flow
        Thread.UncaughtExceptionHandler
            oldHandler = Thread.currentThread().getUncaughtExceptionHandler();
        // Prevent storage cleanup on test failure for postmortem research
        Thread.currentThread().setUncaughtExceptionHandler(
            (t, e) -> {
                keepStorages = true;
                oldHandler.uncaughtException(t, e);
            }
        );

        { // === === === Testing small subsets === === ===
            List<String> smallFile = lines(
                strings("Foobar Candy", "Olympics 2012", "PGA", "CNET", "Microsoft Bing"),
                strings("", "X", ""),
                strings(""),
                strings("Foobar Andy", "Olympics 2014", "FPGA", "C# .NET", "Microsoft", "Xing"),
                strings("Foobar Candy", "Microsoft Bing", "Olympics 2013", "PGA", "PGA", "CNET")
            );

            System.out.println("Check for first top 5 phrases on a small subset");
            performTest(
                smallFile,
                5,
                phrases(
                    phrase(3, "PGA"),
                    phrase(2, "CNET"),
                    phrase(2, "Microsoft Bing"),
                    phrase(2, "Foobar Candy"),
                    phrase(3, "")
                )
            );
            // ------------------------------------------------

            System.out.println("Check for all 14 top phrases on a small subset");
            performTest(
                smallFile,
                14,
                phrases(
                    phrase(1, "Olympics 2012"),
                    phrase(1, "X"),
                    phrase(1, "Foobar Andy"),
                    phrase(1, "Olympics 2014"),
                    phrase(1, "FPGA"),
                    phrase(1, "C# .NET"),
                    phrase(1, "Microsoft"),
                    phrase(1, "Xing"),
                    phrase(1, "Olympics 2013"),
                    phrase(2, "CNET"),
                    phrase(2, "Microsoft Bing"),
                    phrase(2, "Foobar Candy"),
                    phrase(3, ""),
                    phrase(3, "PGA")
                )
            );
        }
        { // === === === One line test === === ===
            System.out.println("Check for all phrases on one line");
            performTest(
                lines(
                    strings("", "hello there", "a b c")
                ),
                3,
                phrases(
                    phrase(1, ""),
                    phrase(1, "hello there"),
                    phrase(1, "a b c")
                )
            );

        }
        { // === === === Several lines test === === ===
            System.out.println("Check for all phrases on three line set");
            performTest(
                lines(
                    strings("", "hello there", "a b c"),
                    strings("", "hello there", "a b c"),
                    strings("", "hello there", "a b c")
                ),
                3,
                phrases(
                    phrase(3, ""),
                    phrase(3, "hello there"),
                    phrase(3, "a b c")
                )
            );

        }
        { // === === === Several lines test === === ===

            System.out.println("Check for all phrases on three line set");
            performTest(
                lines(
                    strings("This is a long long line", "Hello, world!"),
                    strings("a String", "This is sane phrase!", "a String", "Abc defghij klmnopq",
                            "One of those top phrases. (Одна из тех осмысленных фраз)",
                            "p9 p10 p11", "")
                ),
                1,
                phrases(
                    phrase(2, "a String")
                )
            );

        }
        { // === === === Biger file test === === ===

            System.out.println("Check for all phrases on bigger phrases set");

            // Prepare the disposable source file
            Path testFilePath = prepareDisposableFile("BiggerFile.txt");

            Map<String, Integer> accumulator = new HashMap<>();
            IntStream.range(0, 100).forEach(
                i -> {
                    // Build single line
                    String line = strings(
                        IntStream.range(0, 10 + RANDOM.nextInt(20) + 1)
                            .mapToObj(
                                j -> {
                                    int phraseLength = 4 + RANDOM.nextInt(46);
                                    String text = (j == 0)
                                                  ? ""
                                                  : scatterSpaces(generatePhrase(phraseLength));
                                    String encoded = enc(text);
                                    // Accumulate generated phrases in map
                                    if (accumulator.containsKey(encoded)) {
                                        accumulator.put(encoded, accumulator.get(encoded) + 1);
                                    } else {
                                        accumulator.put(encoded, 1);
                                    }

                                    return text;
                                }
                            )
                    );
                    // Write single line (+ line separator) to the file
                    persistSingleLineWithEol(testFilePath, line);
                }
            );

            // Convert map to master sample set to compare with the search results
            Set<Phrase> masterPhrases =
                accumulator.entrySet().stream()
                    .map(entry -> new Phrase(entry.getValue(), entry.getKey()))
                    .collect(Collectors.toCollection(TreeSet::new));

            // Perform the search and compare the results
            assertEquals(
                masterPhrases,
                findAndPrint(testFilePath.toFile().getAbsolutePath(), masterPhrases.size()));
        }
        { // === === === Large file test === === ===

            System.out.println("Check for three top phrases on large set of phrases");

            // Prepare the disposable source file
            Path testFilePath = prepareDisposableFile("LargeFile.txt");

            String[] topTexts = new String[]{
                "Hello, world!",
                "This is sane phrase!",
                "One of those top phrases. (Одна из тех осмысленных фраз)",
                };
            int[] topTextsCounters = new int[]{0, 0, 0};

            IntStream.range(0, 100).forEach(
                i -> {
                    // Build single line
                    String line = strings(
                        IntStream.range(0, 40 + RANDOM.nextInt(50) + 1)
                            .mapToObj(
                                j -> {
                                    int phraseLength = 4 + RANDOM.nextInt(20);
                                    int seed = RANDOM.nextInt(30) + 1;

                                    if (seed > 0 && seed % 10 == 0) {
                                        int idx = seed / 10 - 1;
                                        topTextsCounters[idx]++;
                                        return topTexts[idx];
                                    } else {
                                        return scatterSpaces(generatePhrase(phraseLength));
                                    }
                                }
                            )
                    );
                    // Write single line (+ line separator) to the file
                    persistSingleLineWithEol(testFilePath, line);
                }
            );

            // Build master sample set to compare with the search results
            Set<Phrase> masterPhrases =
                IntStream.range(0, topTexts.length)
                    .mapToObj(i -> new Phrase(topTextsCounters[i], enc(topTexts[i])))
                    .collect(Collectors.toCollection(TreeSet::new));

            // Perform the search and compare the results
            assertEquals(
                masterPhrases,
                findAndPrint(testFilePath.toFile().getAbsolutePath(), topTexts.length));
        }
    }

    /* Tests utility methods. */

    /**
     * Syntax sugar for appending single line to the file.
     */
    private static void persistSingleLineWithEol(Path testFilePath, String line) {
        try {
            Files.write(testFilePath,
                        line.concat(System.getProperty("line.separator")).getBytes(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Syntax sugar to create a disposable file to keep the source data.
     */
    private static Path prepareDisposableFile(String TEST_DATA_FILENAME) {
        Path testFilePath = Paths.get(TEST_DATA_FILENAME);
        testFilePath.toFile().deleteOnExit();
        return testFilePath;
    }

    /**
     * Syntax sugar for dumping on the screen the results of search.
     */
    private static TreeSet<Phrase> findAndPrint(String filename, int topLimit) {

        TreeSet<Phrase> topPhrases = findTopPhrases(filename, topLimit);

        // Dump all phrases to stdout
        topPhrases.forEach(phrase ->
                               System.out.println(
                                   String.format("[%s] \"%s\" (%s)",
                                                 phrase.count,
                                                 readWholeFileAsString(phrase),
                                                 phrase.relativePath))
        );

        return topPhrases;
    }

    private static final Random RANDOM = new Random(System.nanoTime());

    /**
     * Syntax sugar for generating a random character sequence.
     */
    private static String generatePhrase(int len) {
        final char[] alphabet =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

        StringBuilder result = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            result.append(alphabet[RANDOM.nextInt(alphabet.length)]);
        }

        return result.toString();
    }

    /**
     * Syntax sugar for scattering spaces in continuous character sequence.
     */
    private static String scatterSpaces(String phrase) {

        StringBuilder result = new StringBuilder(phrase);

        int length = phrase.length();
        int spacesAmount = (length < 4) ? 0 : 1 + RANDOM.nextInt(length / 4);

        for (int i = 0; i < spacesAmount; i++) {
            int pos;
            do {
                pos = 1 + RANDOM.nextInt(length - 2); // Choose char excluding first and last
            } while (phrase.charAt(pos - 1) == ' ' || phrase.charAt(pos + 1) == ' ');
            result.setCharAt(pos, ' ');
        }
        return result.toString();
    }

    /**
     * Syntax sugar for invoking the check algorithm and dumping the result on the screen.
     */
    private static void performTest(List<String> file,
                                    int topLimit,
                                    TreeSet<Phrase> phrases) throws IOException {

        String TEST_DATA_FILENAME = "test_data.txt";
        generateTheTestFile(TEST_DATA_FILENAME, file);
        assertEquals(phrases, findAndPrint(TEST_DATA_FILENAME, topLimit));
    }

    /**
     * Syntax sugar for producing the phrase text from Phrase container object.
     */
    private static String readWholeFileAsString(Phrase phrase) {
        try {
            return Files.readAllLines(
                // Read all lines
                Paths.get(
                    getStoragePath().toFile().getAbsolutePath(),
                    phrase.relativePath,
                    Integer.toString(phrase.count))
            ).stream()
                // Concatenate all lines into string
                .map(StringBuffer::new)
                .reduce(new StringBuffer(""), StringBuffer::append)
                .toString();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Syntax sugar for encoding the phrase in special digest form.
     */
    private static String enc(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM);
            md.update(str.getBytes());
            return
                StorageUtils
                    .digestToDirectoryStructure(md.digest())
                    .substring(1); // Removing the leading file separator char
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Syntax sugar for generating master sample of single Phrase.
     */
    private static AbstractMap.SimpleEntry<Integer, String> phrase(int count, String phrase) {
        return new AbstractMap.SimpleEntry<>(count, enc(phrase));
    }

    /**
     * Syntax sugar for generating master sample of Phrase objects.
     */
    private static TreeSet<Phrase> phrases(AbstractMap.SimpleEntry<Integer, String>... phrases) {
        TreeSet<Phrase> phrasesMasterSet = new TreeSet<>();
        for (AbstractMap.SimpleEntry<Integer, String> phrase : phrases) {
            phrasesMasterSet.add(new Phrase(phrase.getKey(), phrase.getValue()));
        }
        return phrasesMasterSet;
    }

    /**
     * Syntax sugar for generating the test file with the specified content.
     */
    private static void generateTheTestFile(String filename, List<String> lines) throws
                                                                                 IOException {
        Path path = Paths.get(filename);
        Files.write(path, lines, Charset.forName("UTF-8"));
        path.toFile().deleteOnExit();
    }

    /**
     * Syntax sugar for building list of phrase strings.
     */
    private static List<String> lines(String... lines) {
        return lines(Arrays.stream(lines));
    }

    private static List<String> lines(Stream<String> lines) {
        return lines.collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Syntax sugar for building string of phrases separated with SEPARATOR char.
     */
    private static String strings(String... phrases) {
        return strings(Arrays.stream(phrases));
    }

    private static String strings(Stream<String> phrases) {

        final String delimiter = Character.toString(SEPARATOR);

        return phrases.collect(Collectors.joining(delimiter));
    }
}