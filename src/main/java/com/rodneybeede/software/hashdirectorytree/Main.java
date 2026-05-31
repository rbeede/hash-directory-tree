package com.rodneybeede.software.hashdirectorytree;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;

import org.apache.commons.codec.digest.DigestUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;


/**
 * Output is https://tools.ietf.org/html/rfc4180
 * 
 * @author rbeede
 *
 */
public class Main {
	private static final Logger log = Logger.getLogger(Main.class);
	
	public static void main(final String[] args) throws IOException, InterruptedException {
		if(null == args || args.length != 2) {
			System.err.println("Incorrect number of arguments");
			System.out.println("Usage:  java -jar " + Main.class.getProtectionDomain().getCodeSource().getLocation().getFile() + " <source directory> <csv output file>");
			System.exit(255);
			return;
		}
		
		
		setupLogging();
				
		
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
			public FileVisitResult postVisitDirectory(Path arg0, IOException arg1) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				log.info("Hashing {}", file);
				
				try {
					final FileInputStream fis = new FileInputStream(file.toFile());
					final String hash = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
					fis.close();
					
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
				} catch(final IOException excep) {
					log.error(excep,excep);
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
		

		LogManager.shutdown();;  //Forces log to flush
		
		System.exit(0);  // All good
	}
	
	
	private static void setupLogging() {
		Layout<?> layout = PatternLayout.newBuilder()
				.withPattern("%d{yyyy-MM-dd HH:mm:ss,SSS Z}\t%-5p\tThread=%t\t%c\t%m%n")
				.build();

		ConsoleAppender consoleAppender = ConsoleAppender.newBuilder()
				.setName("Console")
				.setLayout(layout)
				.build();
		consoleAppender.start();

		final File logFile = new File(System.getProperty("user.dir"), "HDT_" + getFormattedDatestamp(null) + ".log");
		FileAppender fileAppender = FileAppender.newBuilder()
				.withFileName(logFile)
				.withName("File")
				.withLayout(layout)
				.build();
		fileAppender.start();

		org.apache.logging.log4j.core.Logger rootLogger =
				(org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();

		rootLogger.addAppender(consoleAppender);
		rootLogger.addAppender(fileAppender);
		rootLogger.setLevel(Level.INFO);


		System.out.println("Logging to " + logFile.getAbsolutePath());
	}
	
	
	private static String getFormattedDatestamp(final Date date) {
		final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_Z");
		
		if(null == date) {
			return dateFormat.format(new Date());
		} else {
			return dateFormat.format(date);
		}
	}
}
