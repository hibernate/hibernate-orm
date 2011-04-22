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
package org.hibernate.service.jdbc.dialect.internal;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.logging.Logger;

import org.hibernate.dialect.Dialect;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.jdbc.dialect.spi.DialectResolver;

/**
 * A {@link DialectResolver} implementation which coordinates resolution by delegating to sub-resolvers.
 *
 * @author Tomoto Shimizu Washio
 * @author Steve Ebersole
 */
public class DialectResolverSet implements DialectResolver {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, DialectResolverSet.class.getName());

	private List<DialectResolver> resolvers;

	public DialectResolverSet() {
		this( new ArrayList<DialectResolver>() );
	}

	public DialectResolverSet(List<DialectResolver> resolvers) {
		this.resolvers = resolvers;
	}

	public DialectResolverSet(DialectResolver... resolvers) {
		this( Arrays.asList( resolvers ) );
	}

	public Dialect resolveDialect(DatabaseMetaData metaData) throws JDBCConnectionException {
		for ( DialectResolver resolver : resolvers ) {
			try {
				Dialect dialect = resolver.resolveDialect( metaData );
				if ( dialect != null ) {
					return dialect;
				}
			}
			catch ( JDBCConnectionException e ) {
				throw e;
			}
			catch ( Exception e ) {
                LOG.exceptionInSubResolver(e.getMessage());
			}
		}
		return null;
	}

	/**
	 * Add a resolver at the end of the underlying resolver list.  The resolver added by this method is at lower
	 * priority than any other existing resolvers.
	 *
	 * @param resolver The resolver to add.
	 */
	public void addResolver(DialectResolver resolver) {
		resolvers.add( resolver );
	}

	/**
	 * Add a resolver at the beginning of the underlying resolver list.  The resolver added by this method is at higher
	 * priority than any other existing resolvers.
	 *
	 * @param resolver The resolver to add.
	 */
	public void addResolverAtFirst(DialectResolver resolver) {
		resolvers.add( 0, resolver );
	}
}
