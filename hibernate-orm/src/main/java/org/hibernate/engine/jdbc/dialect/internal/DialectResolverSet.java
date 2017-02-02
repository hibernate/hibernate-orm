/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.dialect.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * A {@link DialectResolver} implementation which coordinates resolution by delegating to sub-resolvers.
 *
 * @author Tomoto Shimizu Washio
 * @author Steve Ebersole
 */
public class DialectResolverSet implements DialectResolver {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DialectResolverSet.class );

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

	@Override
	public Dialect resolveDialect(DialectResolutionInfo info) {
		for ( DialectResolver resolver : resolvers ) {
			try {
				final Dialect dialect = resolver.resolveDialect( info );
				if ( dialect != null ) {
					return dialect;
				}
			}
			catch ( JDBCConnectionException e ) {
				throw e;
			}
			catch ( Exception e ) {
				LOG.exceptionInSubResolver( e.getMessage() );
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
