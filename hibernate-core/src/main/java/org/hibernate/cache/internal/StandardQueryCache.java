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
package org.hibernate.cache.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.persistence.EntityNotFoundException;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.QueryCache;
import org.hibernate.cache.spi.QueryKey;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.UpdateTimestampsCache;
import org.hibernate.cfg.Settings;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.type.Type;
import org.hibernate.type.TypeHelper;

/**
 * The standard implementation of the Hibernate QueryCache interface.  This
 * implementation is very good at recognizing stale query results and
 * and re-running queries when it detects this condition, recaching the new
 * results.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class StandardQueryCache implements QueryCache {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			StandardQueryCache.class.getName()
	);

   private static final boolean tracing = LOG.isTraceEnabled();

	private QueryResultsRegion cacheRegion;
	private UpdateTimestampsCache updateTimestampsCache;

	public void clear() throws CacheException {
		cacheRegion.evictAll();
	}

	public StandardQueryCache(
			final Settings settings,
			final Properties props,
			final UpdateTimestampsCache updateTimestampsCache,
			String regionName) throws HibernateException {
		if ( regionName == null ) {
			regionName = StandardQueryCache.class.getName();
		}
		String prefix = settings.getCacheRegionPrefix();
		if ( prefix != null ) {
			regionName = prefix + '.' + regionName;
		}
		LOG.startingQueryCache( regionName );

		this.cacheRegion = settings.getRegionFactory().buildQueryResultsRegion( regionName, props );
		this.updateTimestampsCache = updateTimestampsCache;
	}

	@SuppressWarnings({ "UnnecessaryBoxing", "unchecked" })
	public boolean put(
			QueryKey key,
			Type[] returnTypes,
			List result,
			boolean isNaturalKeyLookup,
			SessionImplementor session) throws HibernateException {
		if ( isNaturalKeyLookup && result.isEmpty() ) {
			return false;
		}
		long ts = cacheRegion.nextTimestamp();

		LOG.debugf( "Caching query results in region: %s; timestamp=%s", cacheRegion.getName(), ts );

		List cacheable = new ArrayList( result.size() + 1 );
		logCachedResultDetails( key, null, returnTypes, cacheable );
		cacheable.add( ts );
		final boolean singleResult = returnTypes.length == 1;
		for ( Object aResult : result ) {
			Serializable cacheItem = singleResult ? returnTypes[0].disassemble(
					aResult,
					session,
					null
			) : TypeHelper.disassemble( (Object[]) aResult, returnTypes, null, session, null );
			cacheable.add( cacheItem );
			logCachedResultRowDetails( returnTypes, aResult );
		}

		cacheRegion.put( key, cacheable );
		return true;
	}

	@SuppressWarnings({ "unchecked" })
	public List get(
			QueryKey key,
			Type[] returnTypes,
			boolean isNaturalKeyLookup,
			Set spaces,
			SessionImplementor session) throws HibernateException {
		LOG.debugf( "Checking cached query results in region: %s", cacheRegion.getName() );

		List cacheable = (List) cacheRegion.get( key );
		logCachedResultDetails( key, spaces, returnTypes, cacheable );

		if ( cacheable == null ) {
			LOG.debug( "Query results were not found in cache" );
			return null;
		}

		Long timestamp = (Long) cacheable.get( 0 );
		if ( !isNaturalKeyLookup && !isUpToDate( spaces, timestamp ) ) {
			LOG.debug( "Cached query results were not up-to-date" );
			return null;
		}

		LOG.debug( "Returning cached query results" );
		final boolean singleResult = returnTypes.length == 1;
		for ( int i = 1; i < cacheable.size(); i++ ) {
			if ( singleResult ) {
				returnTypes[0].beforeAssemble( (Serializable) cacheable.get( i ), session );
			}
			else {
				TypeHelper.beforeAssemble( (Serializable[]) cacheable.get( i ), returnTypes, session );
			}
		}
		List result = new ArrayList( cacheable.size() - 1 );
		for ( int i = 1; i < cacheable.size(); i++ ) {
			try {
				if ( singleResult ) {
					result.add( returnTypes[0].assemble( (Serializable) cacheable.get( i ), session, null ) );
				}
				else {
					result.add(
							TypeHelper.assemble( (Serializable[]) cacheable.get( i ), returnTypes, session, null )
					);
				}
				logCachedResultRowDetails( returnTypes, result.get( i - 1 ) );
			}
			catch ( RuntimeException ex ) {
				if ( isNaturalKeyLookup &&
						( UnresolvableObjectException.class.isInstance( ex ) ||
								EntityNotFoundException.class.isInstance( ex ) ) ) {
					//TODO: not really completely correct, since
					//      the uoe could occur while resolving
					//      associations, leaving the PC in an
					//      inconsistent state
					LOG.debug( "Unable to reassemble cached result set" );
					cacheRegion.evict( key );
					return null;
				}
				throw ex;
			}
		}
		return result;
	}

	protected boolean isUpToDate(Set spaces, Long timestamp) {
		LOG.debugf( "Checking query spaces are up-to-date: %s", spaces );
		return updateTimestampsCache.isUpToDate( spaces, timestamp );
	}

	public void destroy() {
		try {
			cacheRegion.destroy();
		}
		catch ( Exception e ) {
			LOG.unableToDestroyQueryCache( cacheRegion.getName(), e.getMessage() );
		}
	}

	public QueryResultsRegion getRegion() {
		return cacheRegion;
	}

	@Override
	public String toString() {
		return "StandardQueryCache(" + cacheRegion.getName() + ')';
	}

	private static void logCachedResultDetails(QueryKey key, Set querySpaces, Type[] returnTypes, List result) {
		if ( !LOG.isTraceEnabled() ) {
			return;
		}
		LOG.trace( "key.hashCode=" + key.hashCode() );
		LOG.trace( "querySpaces=" + querySpaces );
		if ( returnTypes == null || returnTypes.length == 0 ) {
			LOG.trace(
					"Unexpected returnTypes is "
							+ ( returnTypes == null ? "null" : "empty" ) + "! result"
							+ ( result == null ? " is null" : ".size()=" + result.size() )
			);
		}
		else {
			StringBuilder returnTypeInfo = new StringBuilder();
			for ( int i = 0; i < returnTypes.length; i++ ) {
				returnTypeInfo.append( "typename=" )
						.append( returnTypes[i].getName() )
						.append( " class=" )
						.append( returnTypes[i].getReturnedClass().getName() ).append( ' ' );
			}
			LOG.trace( "unexpected returnTypes is " + returnTypeInfo.toString() + "! result" );
		}
	}

	private static void logCachedResultRowDetails(Type[] returnTypes, Object result) {
		if ( !LOG.isTraceEnabled() ) {
			return;
		}
		logCachedResultRowDetails(
				returnTypes,
				( result instanceof Object[] ? (Object[]) result : new Object[] { result } )
		);
	}

	private static void logCachedResultRowDetails(Type[] returnTypes, Object[] tuple) {
		if ( !tracing ) {
			return;
		}
		if ( tuple == null ) {
			LOG.trace( " tuple is null; returnTypes is " + returnTypes == null ? "null" : "Type[" + returnTypes.length + "]" );
			if ( returnTypes != null && returnTypes.length > 1 ) {
				LOG.trace(
						"Unexpected result tuple! tuple is null; should be Object["
								+ returnTypes.length + "]!"
				);
			}
		}
		else {
			if ( returnTypes == null || returnTypes.length == 0 ) {
				LOG.trace(
						"Unexpected result tuple! tuple is null; returnTypes is "
								+ ( returnTypes == null ? "null" : "empty" )
				);
			}
			LOG.trace( " tuple is Object[" + tuple.length + "]; returnTypes is Type[" + returnTypes.length + "]" );
			if ( tuple.length != returnTypes.length ) {
				LOG.trace(
						"Unexpected tuple length! transformer= expected="
								+ returnTypes.length + " got=" + tuple.length
				);
			}
			else {
				for ( int j = 0; j < tuple.length; j++ ) {
					if ( tuple[j] != null && !returnTypes[j].getReturnedClass().isInstance( tuple[j] ) ) {
						LOG.trace(
								"Unexpected tuple value type! transformer= expected="
										+ returnTypes[j].getReturnedClass().getName()
										+ " got="
										+ tuple[j].getClass().getName()
						);
					}
				}
			}
		}
	}
}
