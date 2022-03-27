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

import org.apache.log4j.AsyncAppender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

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
		
		log.info("Source directory (real canonical) is " + sourceDirectory);
		log.info("CSV report output file path (real canonical) is " + csvOutputFilePath);

		
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
				log.info("Hashing " + file);
				
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
				} catch(final IOException excep) {
					log.error(excep,excep);
				}

				
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				log.error("Failed to access:  " + file, exc);
				
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
		final Layout layout = new PatternLayout("%d{yyyy-MM-dd HH:mm:ss,SSS Z}\t%-5p\tThread=%t\t%c\t%m%n");
		
		
		// Use an async logger for speed
		final AsyncAppender asyncAppender = new AsyncAppender();
		asyncAppender.setThreshold(Level.ALL);
		
		Logger.getRootLogger().setLevel(Level.ALL);
		Logger.getRootLogger().addAppender(asyncAppender);
		
		
		// Setup the logger to also log to the console
		final ConsoleAppender consoleAppender = new ConsoleAppender(layout);
		consoleAppender.setEncoding("UTF-8");
		consoleAppender.setThreshold(Level.INFO);
		asyncAppender.addAppender(consoleAppender);
		
		
		// Setup the logger to log into the current working directory
		final File logFile = new File(System.getProperty("user.dir"), getFormattedDatestamp(null) + ".log");
		final FileAppender fileAppender;
		try {
			fileAppender = new FileAppender(layout, logFile.getAbsolutePath());
		} catch (final IOException e) {
			e.printStackTrace();
			log.error(e,e);
			return;
		}
		fileAppender.setEncoding("UTF-8");
		fileAppender.setThreshold(Level.ALL);
		asyncAppender.addAppender(fileAppender);
		
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
