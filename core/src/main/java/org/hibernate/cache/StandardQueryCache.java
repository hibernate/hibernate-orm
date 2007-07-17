//$Id: StandardQueryCache.java 11398 2007-04-10 14:54:07Z steve.ebersole@jboss.com $
package org.hibernate.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.HibernateException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.cfg.Settings;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;

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

	private static final Log log = LogFactory.getLog( StandardQueryCache.class );

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
		log.info( "starting query cache at region: " + regionName );

		this.cacheRegion = settings.getRegionFactory().buildQueryResultsRegion( regionName, props );
		this.updateTimestampsCache = updateTimestampsCache;
	}

	public boolean put(
			QueryKey key,
			Type[] returnTypes,
			List result,
			boolean isNaturalKeyLookup,
			SessionImplementor session) throws HibernateException {
		if ( isNaturalKeyLookup && result.size() == 0 ) {
			return false;
		}
		else {
			Long ts = new Long( session.getTimestamp() );

			if ( log.isDebugEnabled() ) {
				log.debug( "caching query results in region: " + cacheRegion.getName() + "; timestamp=" + ts );
			}

			List cacheable = new ArrayList( result.size() + 1 );
			cacheable.add( ts );
			for ( int i = 0; i < result.size(); i++ ) {
				if ( returnTypes.length == 1 ) {
					cacheable.add( returnTypes[0].disassemble( result.get( i ), session, null ) );
				}
				else {
					cacheable.add(
							TypeFactory.disassemble(
									( Object[] ) result.get( i ), returnTypes, null, session, null
							)
					);
				}
			}

			cacheRegion.put( key, cacheable );

			return true;

		}

	}

	public List get(
			QueryKey key,
			Type[] returnTypes,
			boolean isNaturalKeyLookup,
			Set spaces,
			SessionImplementor session) throws HibernateException {
		if ( log.isDebugEnabled() ) {
			log.debug( "checking cached query results in region: " + cacheRegion.getName() );
		}

		List cacheable = ( List ) cacheRegion.get( key );
		if ( cacheable == null ) {
			log.debug( "query results were not found in cache" );
			return null;
		}

		Long timestamp = ( Long ) cacheable.get( 0 );
		if ( !isNaturalKeyLookup && !isUpToDate( spaces, timestamp ) ) {
			log.debug( "cached query results were not up to date" );
			return null;
		}

		log.debug( "returning cached query results" );
		for ( int i = 1; i < cacheable.size(); i++ ) {
			if ( returnTypes.length == 1 ) {
				returnTypes[0].beforeAssemble( ( Serializable ) cacheable.get( i ), session );
			}
			else {
				TypeFactory.beforeAssemble( ( Serializable[] ) cacheable.get( i ), returnTypes, session );
			}
		}
		List result = new ArrayList( cacheable.size() - 1 );
		for ( int i = 1; i < cacheable.size(); i++ ) {
			try {
				if ( returnTypes.length == 1 ) {
					result.add( returnTypes[0].assemble( ( Serializable ) cacheable.get( i ), session, null ) );
				}
				else {
					result.add(
							TypeFactory.assemble(
									( Serializable[] ) cacheable.get( i ), returnTypes, session, null
							)
					);
				}
			}
			catch ( UnresolvableObjectException uoe ) {
				if ( isNaturalKeyLookup ) {
					//TODO: not really completely correct, since
					//      the uoe could occur while resolving
					//      associations, leaving the PC in an
					//      inconsistent state
					log.debug( "could not reassemble cached result set" );
					cacheRegion.evict( key );
					return null;
				}
				else {
					throw uoe;
				}
			}
		}
		return result;
	}

	protected boolean isUpToDate(Set spaces, Long timestamp) {
		if ( log.isDebugEnabled() ) {
			log.debug( "Checking query spaces for up-to-dateness: " + spaces );
		}
		return updateTimestampsCache.isUpToDate( spaces, timestamp );
	}

	public void destroy() {
		try {
			cacheRegion.destroy();
		}
		catch ( Exception e ) {
			log.warn( "could not destroy query cache: " + cacheRegion.getName(), e );
		}
	}

	public QueryResultsRegion getRegion() {
		return cacheRegion;
	}

	public String toString() {
		return "StandardQueryCache(" + cacheRegion.getName() + ')';
	}

}
