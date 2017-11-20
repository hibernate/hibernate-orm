/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.dialect.internal;

import org.hibernate.dialect.Database;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * The standard DialectResolver implementation
 *
 * @author Steve Ebersole
 */
public class StandardDialectResolver implements DialectResolver {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( StandardDialectResolver.class );

	/**
	 * Singleton access
	 */
	public static final StandardDialectResolver INSTANCE = new StandardDialectResolver();

	@Override
	public Dialect resolveDialect(DialectResolutionInfo info) {

		for ( Database database : Database.values() ) {
			Dialect dialect = database.resolveDialect( info );
			if ( dialect != null ) {
				return dialect;
			}
		}

		return null;
	}
}
