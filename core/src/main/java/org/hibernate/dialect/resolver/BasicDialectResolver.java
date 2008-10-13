/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.dialect.resolver;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.hibernate.dialect.Dialect;
import org.hibernate.HibernateException;

/**
 * Intended as support for custom resolvers.
 *
 * @author Steve Ebersole
 */
public class BasicDialectResolver extends AbstractDialectResolver {
	public static final int VERSION_INSENSITIVE_VERSION = -9999;

	private final String matchingName;
	private final int matchingVersion;
	private final Class dialectClass;

	public BasicDialectResolver(String matchingName, Class dialectClass) {
		this( matchingName, VERSION_INSENSITIVE_VERSION, dialectClass );
	}

	public BasicDialectResolver(String matchingName, int matchingVersion, Class dialectClass) {
		this.matchingName = matchingName;
		this.matchingVersion = matchingVersion;
		this.dialectClass = dialectClass;
	}

	protected final Dialect resolveDialectInternal(DatabaseMetaData metaData) throws SQLException {
		final String databaseName = metaData.getDatabaseProductName();
		final int databaseMajorVersion = metaData.getDatabaseMajorVersion();

		if ( matchingName.equalsIgnoreCase( databaseName )
				&& ( matchingVersion == VERSION_INSENSITIVE_VERSION || matchingVersion == databaseMajorVersion ) ) {
			try {
				return ( Dialect ) dialectClass.newInstance();
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
