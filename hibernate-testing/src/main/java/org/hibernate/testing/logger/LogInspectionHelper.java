/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.logger;

import java.lang.reflect.Field;

import org.hibernate.AssertionFailure;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.DelegatingBasicLogger;

/**
 * Test helper to listen for logging events.
 * For this to work, it requires JBoss Logging to pick up our custom
 * implementation {@code Log4DelegatingLogger} via ServiceLoader.
 *
 * @author Sanne Grinovero (C) 2015 Red Hat Inc.
 */
public final class LogInspectionHelper {

	private LogInspectionHelper() {
	}

	public static void registerListener(LogListener listener, BasicLogger log) {
		convertType( log ).registerListener( listener );
	}

	public static void clearAllListeners(BasicLogger log) {
		convertType( log ).clearAllListeners();
	}

	private static Log4J2DelegatingLogger convertType(BasicLogger log) {
		if ( log instanceof DelegatingBasicLogger) {
			//Most loggers generated via the annotation processor are of this type
			DelegatingBasicLogger wrapper = (DelegatingBasicLogger) log;
			try {
				return extractFromWrapper( wrapper );
			}
			catch (Exception cause) {
				throw new RuntimeException( cause );
			}
		}
		if ( ! ( log instanceof Log4J2DelegatingLogger ) ) {
			throw new AssertionFailure( "Unexpected log type: JBoss Logger didn't register the custom TestableLoggerProvider as logger provider" );
		}
		return (Log4J2DelegatingLogger) log;
	}

	private static Log4J2DelegatingLogger extractFromWrapper(DelegatingBasicLogger wrapper) throws Exception {
		Field field = DelegatingBasicLogger.class.getDeclaredField( "log" );
		field.setAccessible( true );
		Object object = field.get( wrapper );
		return convertType( (BasicLogger) object );
	}

}
