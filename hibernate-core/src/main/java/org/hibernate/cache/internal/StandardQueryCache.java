/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.persistence.EntityNotFoundException;

import org.hibernate.HibernateException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.QueryCache;
import org.hibernate.cache.spi.QueryKey;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.UpdateTimestampsCache;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.type.Type;
import org.hibernate.type.TypeHelper;

import org.jboss.logging.Logger;

/**
 * The standard implementation of the Hibernate QueryCache interface.  This
 * implementation is very good at recognizing stale query results and
 * and re-running queries when it detects this condition, re-caching the new
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

	private static final boolean DEBUGGING = LOG.isDebugEnabled();
	private static final boolean TRACING = LOG.isTraceEnabled();

	private QueryResultsRegion cacheRegion;
	private UpdateTimestampsCache updateTimestampsCache;

	/**
	 * Constructs a StandardQueryCache instance
	 *
	 * @param settings The SessionFactory settings.
	 * @param props Any properties
	 * @param updateTimestampsCache The update-timestamps cache to use.
	 * @param regionName The base query cache region name
	 */
	public StandardQueryCache(
			final SessionFactoryOptions settings,
			final Properties props,
			final UpdateTimestampsCache updateTimestampsCache,
			final String regionName) {
		String regionNameToUse = regionName;
		if ( regionNameToUse == null ) {
			regionNameToUse = StandardQueryCache.class.getName();
		}
		final String prefix = settings.getCacheRegionPrefix();
		if ( prefix != null ) {
			regionNameToUse = prefix + '.' + regionNameToUse;
		}
		LOG.startingQueryCache( regionNameToUse );

		this.cacheRegion = settings.getServiceRegistry().getService( RegionFactory.class ).buildQueryResultsRegion(
				regionNameToUse,
				props
		);
		this.updateTimestampsCache = updateTimestampsCache;
	}

	@Override
	public QueryResultsRegion getRegion() {
		return cacheRegion;
	}

	@Override
	public void destroy() {
		try {
			cacheRegion.destroy();
		}
		catch ( Exception e ) {
			LOG.unableToDestroyQueryCache( cacheRegion.getName(), e.getMessage() );
		}
	}

	@Override
	public void clear() throws CacheException {
		cacheRegion.evictAll();
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public boolean put(
			final QueryKey key,
			final Type[] returnTypes,
			final List result,
			final boolean isNaturalKeyLookup,
			final SessionImplementor session) throws HibernateException {
		if ( isNaturalKeyLookup && result.isEmpty() ) {
			return false;
		}
		final long ts = cacheRegion.nextTimestamp();

		if ( DEBUGGING ) {
			LOG.debugf( "Caching query results in region: %s; timestamp=%s", cacheRegion.getName(), ts );
		}

		final List cacheable = new ArrayList( result.size() + 1 );
		logCachedResultDetails( key, null, returnTypes, cacheable );
		cacheable.add( ts );

		final boolean isSingleResult = returnTypes.length == 1;
		for ( Object aResult : result ) {
			final Serializable cacheItem = isSingleResult
					? returnTypes[0].disassemble( aResult, session, null )
					: TypeHelper.disassemble( (Object[]) aResult, returnTypes, null, session, null );
			cacheable.add( cacheItem );
			logCachedResultRowDetails( returnTypes, aResult );
		}

		try {
			session.getEventListenerManager().cachePutStart();
			cacheRegion.put( key, cacheable );
		}
		finally {
			session.getEventListenerManager().cachePutEnd();
		}

		return true;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public List get(
			final QueryKey key,
			final Type[] returnTypes,
			final boolean isNaturalKeyLookup,
			final Set<Serializable> spaces,
			final SessionImplementor session) throws HibernateException {
		if ( DEBUGGING ) {
			LOG.debugf( "Checking cached query results in region: %s", cacheRegion.getName() );
		}

		final List cacheable = getCachedResults( key, session );
		logCachedResultDetails( key, spaces, returnTypes, cacheable );

		if ( cacheable == null ) {
			if ( DEBUGGING ) {
				LOG.debug( "Query results were not found in cache" );
			}
			return null;
		}

		final Long timestamp = (Long) cacheable.get( 0 );
		if ( !isNaturalKeyLookup && !isUpToDate( spaces, timestamp, session ) ) {
			if ( DEBUGGING ) {
				LOG.debug( "Cached query results were not up-to-date" );
			}
			return null;
		}

		if ( DEBUGGING ) {
			LOG.debug( "Returning cached query results" );
		}
		final boolean singleResult = returnTypes.length == 1;
		for ( int i = 1; i < cacheable.size(); i++ ) {
			if ( singleResult ) {
				returnTypes[0].beforeAssemble( (Serializable) cacheable.get( i ), session );
			}
			else {
				TypeHelper.beforeAssemble( (Serializable[]) cacheable.get( i ), returnTypes, session );
			}
		}

		final List result = new ArrayList( cacheable.size() - 1 );
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
				if ( isNaturalKeyLookup ) {
					// potentially perform special handling for natural-id look ups.
					if ( UnresolvableObjectException.class.isInstance( ex )
							|| EntityNotFoundException.class.isInstance( ex ) ) {
						if ( DEBUGGING ) {
							LOG.debug( "Unable to reassemble cached natural-id query result" );
						}
						cacheRegion.evict( key );

						// EARLY EXIT !!!!!
						return null;
					}
				}
				throw ex;
			}
		}
		return result;
	}

	private List getCachedResults(QueryKey key, SessionImplementor session) {
		List cacheable = null;
		try {
			session.getEventListenerManager().cacheGetStart();
			cacheable = (List) cacheRegion.get( key );
		}
		finally {
			session.getEventListenerManager().cacheGetEnd( cacheable != null );
		}
		return cacheable;
	}


	protected boolean isUpToDate(Set<Serializable> spaces, Long timestamp, SessionImplementor session) {
		if ( DEBUGGING ) {
			LOG.debugf( "Checking query spaces are up-to-date: %s", spaces );
		}
		return updateTimestampsCache.isUpToDate( spaces, timestamp, session );
	}

	@Override
	public String toString() {
		return "StandardQueryCache(" + cacheRegion.getName() + ')';
	}

	private static void logCachedResultDetails(QueryKey key, Set querySpaces, Type[] returnTypes, List result) {
		if ( !TRACING ) {
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
			final StringBuilder returnTypeInfo = new StringBuilder();
			for ( Type returnType : returnTypes ) {
				returnTypeInfo.append( "typename=" )
						.append( returnType.getName() )
						.append( " class=" )
						.append( returnType.getReturnedClass().getName() )
						.append( ' ' );
			}
			LOG.trace( "unexpected returnTypes is " + returnTypeInfo.toString() + "! result" );
		}
	}

	private static void logCachedResultRowDetails(Type[] returnTypes, Object result) {
		if ( !TRACING ) {
			return;
		}
		logCachedResultRowDetails(
				returnTypes,
				( result instanceof Object[] ? (Object[]) result : new Object[] { result } )
		);
	}

	private static void logCachedResultRowDetails(Type[] returnTypes, Object[] tuple) {
		if ( !TRACING ) {
			return;
		}
		if ( tuple == null ) {
			LOG.tracef(
					"tuple is null; returnTypes is %s",
					returnTypes == null ? "null" : "Type[" + returnTypes.length + "]"
			);
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
			LOG.tracef(
					"tuple is Object[%s]; returnTypes is %s",
					tuple.length,
					returnTypes == null ? "null" : "Type[" + returnTypes.length + "]"
			);
			if ( returnTypes != null && tuple.length != returnTypes.length ) {
				LOG.trace(
						"Unexpected tuple length! transformer= expected="
								+ returnTypes.length + " got=" + tuple.length
				);
			}
			else {
				for ( int j = 0; j < tuple.length; j++ ) {
					if ( tuple[j] != null && returnTypes != null
							&& ! returnTypes[j].getReturnedClass().isInstance( tuple[j] ) ) {
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
