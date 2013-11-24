/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cache.ehcache.internal.nonstop;

import net.sf.ehcache.constructs.nonstop.NonStopCacheException;

import org.hibernate.cache.ehcache.EhCacheMessageLogger;

import org.jboss.logging.Logger;

/**
 * Class that takes care of {@link net.sf.ehcache.constructs.nonstop.NonStopCacheException} that happens in hibernate module
 *
 * @author Abhishek Sanoujam
 * @author Alex Snaps
 */
public final class HibernateNonstopCacheExceptionHandler {
	/**
	 * Property name which set as "true" will throw exceptions on timeout with hibernate
	 */
	public static final String HIBERNATE_THROW_EXCEPTION_ON_TIMEOUT_PROPERTY = "ehcache.hibernate.propagateNonStopCacheException";

	/**
	 * Property name for logging the stack trace of the nonstop cache exception too. False by default
	 */
	public static final String HIBERNATE_LOG_EXCEPTION_STACK_TRACE_PROPERTY = "ehcache.hibernate.logNonStopCacheExceptionStackTrace";

	private static final EhCacheMessageLogger LOG = Logger.getMessageLogger(
			EhCacheMessageLogger.class,
			HibernateNonstopCacheExceptionHandler.class.getName()
	);
	private static final HibernateNonstopCacheExceptionHandler INSTANCE = new HibernateNonstopCacheExceptionHandler();

	/**
	 * private constructor
	 */
	private HibernateNonstopCacheExceptionHandler() {
		// private
	}

	/**
	 * Returns the singleton instance
	 *
	 * @return the singleton instance
	 */
	public static HibernateNonstopCacheExceptionHandler getInstance() {
		return INSTANCE;
	}

	/**
	 * Handle {@link net.sf.ehcache.constructs.nonstop.NonStopCacheException}.
	 * If {@link HibernateNonstopCacheExceptionHandler#HIBERNATE_THROW_EXCEPTION_ON_TIMEOUT_PROPERTY} system property is set to true,
	 * rethrows the {@link net.sf.ehcache.constructs.nonstop.NonStopCacheException}, otherwise logs the exception. While logging, if
	 * {@link HibernateNonstopCacheExceptionHandler#HIBERNATE_LOG_EXCEPTION_STACK_TRACE_PROPERTY} is set to true, logs the exception stack
	 * trace too, otherwise logs the exception message only
	 *
	 * @param nonStopCacheException The exception to handle
	 */
	public void handleNonstopCacheException(NonStopCacheException nonStopCacheException) {
		if ( Boolean.getBoolean( HIBERNATE_THROW_EXCEPTION_ON_TIMEOUT_PROPERTY ) ) {
			throw nonStopCacheException;
		}
		else {
			if ( Boolean.getBoolean( HIBERNATE_LOG_EXCEPTION_STACK_TRACE_PROPERTY ) ) {
				LOG.debug(
						"Ignoring NonstopCacheException - " + nonStopCacheException.getMessage(),
						nonStopCacheException
				);
			}
			else {
				LOG.debug( "Ignoring NonstopCacheException - " + nonStopCacheException.getMessage() );
			}
		}
	}
}
