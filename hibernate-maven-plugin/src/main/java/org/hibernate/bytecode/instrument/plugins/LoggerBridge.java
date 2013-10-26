package com.awesomeware.hibernate.plugin;

import org.codehaus.plexus.logging.Logger;

/**
 * A bridge to allow Maven's plexus logging to be used in Hibernate's buildtime tasks.
 */
public final class LoggerBridge implements org.hibernate.bytecode.buildtime.spi.Logger {
	private final Logger mavenLogger;
	
	/**
	 * Constructs a bridge, using a provided Maven plexus logger to direct logger output to.
	 * 
	 * @param mavenLogger The Maven plexus logger to direct logger output to.
	 */
	public LoggerBridge(final Logger mavenLogger) {
		this.mavenLogger = mavenLogger;
	}

	/* (non-Javadoc)
	 * @see org.hibernate.bytecode.buildtime.spi.Logger#trace()
	 */
	@Override
	public void trace(String message) {}

	/* (non-Javadoc)
	 * @see org.hibernate.bytecode.buildtime.spi.Logger#debug()
	 */
	@Override
	public void debug(String message) {
		mavenLogger.debug(message);
	}

	/* (non-Javadoc)
	 * @see org.hibernate.bytecode.buildtime.spi.Logger#info()
	 */
	@Override
	public void info(String message) {
		mavenLogger.info(message);
	}

	/* (non-Javadoc)
	 * @see org.hibernate.bytecode.buildtime.spi.Logger#warn()
	 */
	@Override
	public void warn(String message) {
		mavenLogger.warn(message);
	}

	/* (non-Javadoc)
	 * @see org.hibernate.bytecode.buildtime.spi.Logger#error()
	 */
	@Override
	public void error(String message) {
		mavenLogger.error(message);
	}
}
