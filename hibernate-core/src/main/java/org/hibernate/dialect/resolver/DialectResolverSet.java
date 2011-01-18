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
package org.hibernate.dialect.resolver;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.hibernate.cfg.CollectionSecondPass;
import org.hibernate.dialect.Dialect;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.service.jdbc.dialect.spi.DialectResolver;

/**
 * A {@link DialectResolver} implementation which coordinates resolution by delegating to its
 * registered sub-resolvers.  Sub-resolvers may be registered by calling either {@link #addResolver} or
 * {@link #addResolverAtFirst}.
 *
 * @author Tomoto Shimizu Washio
 */
public class DialectResolverSet implements DialectResolver {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                CollectionSecondPass.class.getPackage().getName());

	private List resolvers = new ArrayList();

	/**
	 * {@inheritDoc}
	 */
	public Dialect resolveDialect(DatabaseMetaData metaData) {
		Iterator i = resolvers.iterator();
		while ( i.hasNext() ) {
			final DialectResolver resolver = ( DialectResolver ) i.next();
			try {
				Dialect dialect = resolver.resolveDialect( metaData );
				if ( dialect != null ) {
					return dialect;
				}
			}
			catch ( JDBCConnectionException e ) {
				throw e;
			}
			catch ( Throwable t ) {
                LOG.subResolverException(t.getMessage());
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
