package com.rodneybeede.software.hashdirectorytree;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.commons.codec.digest.DigestUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Output is https://tools.ietf.org/html/rfc4180
 *
 * Simplified logging: configuration is provided via src/main/resources/log4j2.xml on the classpath.
 *
 * @author rbeede
 */
public class Main {
    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(final String[] args) throws IOException, InterruptedException {
        if (null == args || args.length != 2) {
            System.err.println("Incorrect number of arguments");
            System.out.println("Usage:  java -jar " + Main.class.getProtectionDomain().getCodeSource().getLocation().getFile() + " <source directory> <csv output file>");
            System.exit(255);
            return;
        }

        // Logging is configured via src/main/resources/log4j2.xml on the classpath

        // Parse config options as Canonical paths
        final Path sourceDirectory = Paths.get(args[0]).toRealPath();
        final Path csvOutputFilePath = Paths.get(args[1]).toAbsolutePath();  // RealPath doesn't exist yet

        log.info("Source directory (real canonical) is {}", sourceDirectory);
        log.info("CSV report output file path (real canonical) is {}", csvOutputFilePath);

        final FileWriter fwriter = new FileWriter(csvOutputFilePath.toFile());
        fwriter.write("Size-bytes,Hash,File");  // HEADER
        // Don't write the \r\n yet as we do that for each row we add
        // Saves us from having to leave-off a trailing \r\n at the very end

        Files.walkFileTree(sourceDirectory, new FileVisitor<Path>() {

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                log.info("Hashing {}", file);

                try (FileInputStream fis = new FileInputStream(file.toFile())) {
                    final String hash = DigestUtils.md5Hex(fis);

                    fwriter.write("\r\n");

                    fwriter.write(Long.toString(Files.size(file)));
                    fwriter.write(',');
                    fwriter.write(hash);
                    fwriter.write(',');
                    fwriter.write('"');  // double quote the third field
                    fwriter.write(file.toString().replace("\"", "\"\""));  // All internal " with double  ""
                    fwriter.write('"');  // close of third field
                    // Don't write \r\n here, only for next row if any (no trailing \r\n at end of file)
                    fwriter.flush();
                } catch (final IOException excep) {
                    log.error("Error hashing file {}", file, excep);
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                log.error("Failed to access:  {}", file, exc);

                return FileVisitResult.CONTINUE;
            }

        });

        fwriter.close();  // flushes too

        // Exit with appropriate status
        log.info("Program has completed");

        System.exit(0);  // All good
    }
}
