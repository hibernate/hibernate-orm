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

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class QueryHintDefinition {
	private final Map<String, Object> hintsMap;

	public QueryHintDefinition(final QueryHint[] hints) {
		if ( hints == null || hints.length == 0 ) {
			hintsMap = Collections.emptyMap();
		}
		else {
			final Map<String, Object> hintsMap = new HashMap<String, Object>();
			for ( QueryHint hint : hints ) {
				hintsMap.put( hint.name(), hint.value() );
			}
			this.hintsMap = hintsMap;
		}
	}


	public CacheMode getCacheMode(String query) {
		String hitName = QueryHints.CACHE_MODE;
		String value =(String) hintsMap.get( hitName );
		if ( value == null ) {
			return null;
		}
		try {
			return CacheMode.interpretExternalSetting( value );
		}
		catch ( MappingException e ) {
			throw new AnnotationException( "Unknown CacheMode in hint: " + query + ":" + hitName, e );
		}
	}

	public FlushMode getFlushMode(String query) {
		String hitName = QueryHints.FLUSH_MODE;
		String value =(String)  hintsMap.get( hitName );
		if ( value == null ) {
			return null;
		}
		try {
			return FlushMode.interpretExternalSetting( value );
		}
		catch ( MappingException e ) {
			throw new AnnotationException( "Unknown FlushMode in hint: " + query + ":" + hitName, e );
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

	public boolean getBoolean(String query, String hintName) {
		String value =(String)  hintsMap.get( hintName );
		if ( value == null ) {
			return false;
		}
		if ( value.equalsIgnoreCase( "true" ) ) {
			return true;
		}
		else if ( value.equalsIgnoreCase( "false" ) ) {
			return false;
		}
		else {
			throw new AnnotationException( "Not a boolean in hint: " + query + ":" + hintName );
		}

	}

	public String getString(String query, String hintName) {
		return (String) hintsMap.get( hintName );
	}

	public Integer getInteger(String query, String hintName) {
		String value = (String) hintsMap.get( hintName );
		if ( value == null ) {
			return null;
		}
		try {
			return Integer.decode( value );
		}
		catch ( NumberFormatException nfe ) {
			throw new AnnotationException( "Not an integer in hint: " + query + ":" + hintName, nfe );
		}
	}

	public Integer getTimeout(String queryName) {
		Integer timeout = getInteger( queryName, QueryHints.TIMEOUT_JPA );

		if ( timeout != null ) {
			// convert milliseconds to seconds
			timeout = (int) Math.round( timeout.doubleValue() / 1000.0 );
		}
		else {
			// timeout is already in seconds
			timeout = getInteger( queryName, QueryHints.TIMEOUT_HIBERNATE );
		}
		return timeout;
	}

	public LockOptions determineLockOptions(NamedQuery namedQueryAnnotation) {
		LockModeType lockModeType = namedQueryAnnotation.lockMode();
		Integer lockTimeoutHint = getInteger( namedQueryAnnotation.name(), "javax.persistence.lock.timeout" );
		Boolean followOnLocking = getBoolean( namedQueryAnnotation.name(), QueryHints.FOLLOW_ON_LOCKING );

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

	public Map<String, Object> getHintsMap() {
		return hintsMap;
	}
}
