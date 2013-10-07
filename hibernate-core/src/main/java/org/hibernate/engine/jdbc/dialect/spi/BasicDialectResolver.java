/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
