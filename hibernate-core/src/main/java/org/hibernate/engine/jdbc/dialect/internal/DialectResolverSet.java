/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.dialect.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.exception.JDBCConnectionException;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;

/**
 * A {@link DialectResolver} implementation which coordinates resolution by delegating to sub-resolvers.
 *
 * @author Tomoto Shimizu Washio
 * @author Steve Ebersole
 */
public class DialectResolverSet implements DialectResolver {

	private final List<DialectResolver> resolvers;

	public DialectResolverSet() {
		this( new ArrayList<>() );
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
				CORE_LOGGER.exceptionInSubResolver( e.getMessage() );
			}
		}

		return null;
	}

	public void addResolver(DialectResolver... resolvers) {
		this.resolvers.addAll( Arrays.asList( resolvers ) );
	}

	public void addResolverAtFirst(DialectResolver... resolvers) {
		this.resolvers.addAll( 0, Arrays.asList( resolvers ) );
	}

	public void addDiscoveredResolvers(Collection<DialectResolver> resolvers) {
		this.resolvers.addAll( 0, resolvers );
	}
}
