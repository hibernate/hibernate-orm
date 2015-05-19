/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.dialect.spi;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;

import static org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo.NO_VERSION;

/**
 * Intended as support for custom resolvers which match a single db name (with optional version info).
 *
 * @author Steve Ebersole
 */
public class BasicDialectResolver implements DialectResolver {
	private final String nameToMatch;
	private final int majorVersionToMatch;
	private final int minorVersionToMatch;

	private final Class dialectClass;

	/**
	 * Constructs a BasicDialectResolver
	 *
	 * @param nameToMatch The name of the driver to match on
	 * @param dialectClass The Dialect class to use on match
	 */
	public BasicDialectResolver(String nameToMatch, Class dialectClass) {
		this( nameToMatch, NO_VERSION, dialectClass );
	}

	/**
	 * Constructs a BasicDialectResolver
	 *
	 * @param nameToMatch The name of the driver to match on
	 * @param majorVersionToMatch The version of the driver to match on
	 * @param dialectClass The Dialect class to use on match
	 */
	public BasicDialectResolver(String nameToMatch, int majorVersionToMatch, Class dialectClass) {
		this( nameToMatch, majorVersionToMatch, NO_VERSION, dialectClass );
	}

	/**
	 * Constructs a BasicDialectResolver
	 *
	 * @param nameToMatch The name of the driver to match on
	 * @param majorVersionToMatch The version of the driver to match on
	 * @param dialectClass The Dialect class to use on match
	 */
	public BasicDialectResolver(String nameToMatch, int majorVersionToMatch, int minorVersionToMatch, Class dialectClass) {
		this.nameToMatch = nameToMatch;
		this.majorVersionToMatch = majorVersionToMatch;
		this.minorVersionToMatch = minorVersionToMatch;
		this.dialectClass = dialectClass;
	}

	@Override
	public final Dialect resolveDialect(DialectResolutionInfo info) {
		final String databaseName = info.getDatabaseName();
		final int databaseMajorVersion = info.getDatabaseMajorVersion();
		final int databaseMinorVersion = info.getDatabaseMinorVersion();

		if ( nameToMatch.equalsIgnoreCase( databaseName )
				&& ( majorVersionToMatch == NO_VERSION || majorVersionToMatch == databaseMajorVersion )
				&& ( minorVersionToMatch == NO_VERSION || majorVersionToMatch == databaseMinorVersion ) ) {
			try {
				return (Dialect) dialectClass.newInstance();
			}
			catch ( HibernateException e ) {
				// conceivable that the dialect ctor could throw HibernateExceptions, so don't re-wrap
				throw e;
			}
			catch ( Throwable t ) {
				throw new HibernateException(
						"Could not instantiate specified Dialect class [" + dialectClass.getName() + "]",
						t
				);
			}
		}

		return null;
	}
}
