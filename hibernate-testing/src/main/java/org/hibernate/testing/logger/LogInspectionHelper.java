/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2015 Red Hat Inc.
 */
final class LogInspectionHelper {

	private LogInspectionHelper() {
	}

	static void registerListener(LogListener listener, BasicLogger log) {
		convertType( log ).registerListener( listener );
	}

	static void clearAllListeners(BasicLogger log) {
		convertType( log ).clearAllListeners();
	}

	private static Log4DelegatingLogger convertType(BasicLogger log) {
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
		if ( ! ( log instanceof Log4DelegatingLogger ) ) {
			throw new AssertionFailure( "Unexpected log type: JBoss Logger didn't register the custom TestableLoggerProvider as logger provider" );
		}
		return (Log4DelegatingLogger) log;
	}

	private static Log4DelegatingLogger extractFromWrapper(DelegatingBasicLogger wrapper) throws Exception {
		Field field = DelegatingBasicLogger.class.getDeclaredField( "log" );
		field.setAccessible( true );
		Object object = field.get( wrapper );
		return convertType( (BasicLogger) object );
	}

}
