/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.LockModeType;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;

import org.hibernate.AnnotationException;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.annotations.QueryHints;
import org.hibernate.internal.util.LockModeConverter;
import org.hibernate.internal.util.config.ConfigurationHelper;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class QueryHintDefinition {
	private final String queryName;
	private final Map<String, Object> hintsMap;

	public QueryHintDefinition(String queryName, final QueryHint[] hints) {
		this.queryName = queryName;
		if ( hints == null || hints.length == 0 ) {
			hintsMap = Collections.emptyMap();
		}
		else {
			final Map<String, Object> hintsMap = new HashMap<>();
			for ( QueryHint hint : hints ) {
				hintsMap.put( hint.name(), hint.value() );
			}
			this.hintsMap = hintsMap;
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Generic access

	public Map<String, Object> getHintsMap() {
		return hintsMap;
	}

	public String getString(String hintName) {
		return (String) hintsMap.get( hintName );
	}

	public boolean getBoolean(String hintName) {
		try {
			return ConfigurationHelper.getBoolean( hintName, hintsMap );
		}
		catch (Exception e) {
			throw new AnnotationException( "Named query hint [" + hintName + "] is not a boolean: " + queryName, e );
		}
	}

	public Integer getInteger(String hintName) {
		try {
			return ConfigurationHelper.getInteger( hintName, hintsMap );
		}
		catch (Exception e) {
			throw new AnnotationException( "Named query hint [" + hintName + "] is not an integer: " + queryName, e );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Specialized access

	public Integer getTimeout() {
		final Integer jpaTimeout = getInteger( QueryHints.TIMEOUT_JPA );
		if ( jpaTimeout != null ) {
			// convert milliseconds to seconds
			return (int) Math.round( jpaTimeout.doubleValue() / 1000.0 );
		}

		return getInteger( QueryHints.TIMEOUT_HIBERNATE );
	}

	public boolean getCacheability() {
		return getBoolean( QueryHints.CACHEABLE );
	}

	public CacheMode getCacheMode() {
		final String value = getString( QueryHints.CACHE_MODE );
		try {
			return value == null
					? null
					: CacheMode.interpretExternalSetting( value );
		}
		catch (Exception e) {
			throw new AnnotationException( "Unable to interpret CacheMode in named query hint: " + queryName, e );
		}
	}

	public FlushMode getFlushMode() {
		final String value = getString( QueryHints.FLUSH_MODE );
		try {
			return value == null
					? null
					: FlushMode.interpretExternalSetting( value );
		}
		catch (MappingException e) {
			throw new AnnotationException( "Unable to interpret FlushMode in named query hint: " + queryName, e );
		}
	}

	public LockMode getLockMode(String query) {
		String hitName = QueryHints.NATIVE_LOCKMODE;
		String value =(String) hintsMap.get( hitName );
		if ( value == null ) {
			return null;
		}
		try {
			return LockMode.fromExternalForm( value );
		}
		catch ( MappingException e ) {
			throw new AnnotationException( "Unknown LockMode in hint: " + query + ":" + hitName, e );
		}
	}

	public LockOptions determineLockOptions(NamedQuery namedQueryAnnotation) {
		LockModeType lockModeType = namedQueryAnnotation.lockMode();
		Integer lockTimeoutHint = getInteger( "javax.persistence.lock.timeout" );
		Boolean followOnLocking = getBoolean( QueryHints.FOLLOW_ON_LOCKING );

		return determineLockOptions(lockModeType, lockTimeoutHint, followOnLocking);
	}

	private LockOptions determineLockOptions(LockModeType lockModeType, Integer lockTimeoutHint, Boolean followOnLocking) {

		LockOptions lockOptions = new LockOptions( LockModeConverter.convertToLockMode( lockModeType ) )
				.setFollowOnLocking( followOnLocking );
		if ( lockTimeoutHint != null ) {
			lockOptions.setTimeOut( lockTimeoutHint );
		}

		return lockOptions;
	}
}
