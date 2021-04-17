package info.moroff.base.logging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Utils {

	private static final String CONSOLE_HANDLER_LEVEL = "java.util.logging.ConsoleHandler.level";
	private static final String FILE_HANDLER_PATTERN = "java.util.logging.FileHandler.pattern";
	private static final String LOGGING_PROPERTIES = "logging.properties";
	static boolean loggingInitialized = false;

	/**
	 * Setup java utils logger.
	 * @param clazz to be logged.
	 * @return java util logger for the given class.
	 * 
	 */
	public static Logger setupAndCreateLogger(Class<?> clazz) {
		return setupAndCreateLogger(clazz, false, ".", ".");
	}
	
	/**
	 * Setup java utils logger.
	 * 
	 * @param clazz
	 * @param silent
	 * @param configurationPath
	 * @param loggingDirectory
	 * @return
	 */
	public static Logger setupAndCreateLogger(Class<?> clazz, boolean silent, String configurationPath, String loggingDirectory) {
		Logger logger = null;
		String loggerName = clazz != null ? clazz.getName() : "";
		LogManager logManager = LogManager.getLogManager();

		if (!loggingInitialized) {
			URL logConfigURL = null;
			File logConfigFile = new File(configurationPath, LOGGING_PROPERTIES);
			if (logConfigFile.exists()) {
				try {
					logConfigURL = logConfigFile.toURI().toURL();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			} else {
				ClassLoader classLoader = clazz != null ? clazz.getClassLoader() : Thread.currentThread().getContextClassLoader();
				logConfigURL = classLoader.getResource(LOGGING_PROPERTIES);
			}

			if (logConfigURL != null) {
				try (InputStream logConfigStream = logConfigURL.openStream()) {
					Properties properties = new Properties();
					properties.load(logConfigStream);
					
					if ( silent ) {
						properties.setProperty(CONSOLE_HANDLER_LEVEL, Level.SEVERE.getName());
					}
					
					if ( !".".equals(loggingDirectory) ) {
						String pattern = properties.getProperty(FILE_HANDLER_PATTERN);
						
						if ( pattern != null ) {
							int i = pattern.lastIndexOf('/');
							
							if ( i >= 0 ) {
								pattern = loggingDirectory + pattern.substring(i);
							}
							else {
								pattern = loggingDirectory + pattern;
							}
						}
						else {
							pattern = loggingDirectory + "/raincontrol_%u.log";
						}
						properties.setProperty(FILE_HANDLER_PATTERN, pattern);
						
						File loggingPath = new File(loggingDirectory);
						if ( !loggingPath.exists() ) {
							loggingPath.mkdirs();
						}
					}
					
					ByteArrayOutputStream proepertiesOutput = new ByteArrayOutputStream();
					properties.store(proepertiesOutput, null);
					
					ByteArrayInputStream propertiesInput = new ByteArrayInputStream(proepertiesOutput.toByteArray());
					
					logManager.readConfiguration(propertiesInput);
					logger = Logger.getLogger(loggerName);
					logger.info("Logging initialized from " + logConfigURL.toExternalForm());
					logger.fine(proepertiesOutput.toString());
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				logger = Logger.getLogger(loggerName);
				logger.warning(LOGGING_PROPERTIES + " not found, use default logging configuration");
			}
			
			loggingInitialized = true;
		}

		if (logger == null) {
			logger = Logger.getLogger(loggerName);
		}

		if ( logManager != null && silent ) {
			Logger rootLogger = Logger.getLogger("");
			
			if ( rootLogger != null && rootLogger.getParent() == null ) {
				for (Handler logHandler : rootLogger.getHandlers()) {
					if ( logHandler.getClass() == ConsoleHandler.class ) {
						logger.info("Silent mode, set console handler to "+Level.SEVERE);
						logHandler.setLevel(Level.SEVERE);
					}
				}  
			}
		}
		
		return logger;
	}

}
