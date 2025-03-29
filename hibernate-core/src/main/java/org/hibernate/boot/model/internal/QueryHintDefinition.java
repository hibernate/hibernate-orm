/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.LockModeConverter;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.jpa.LegacySpecHints;
import org.hibernate.jpa.SpecHints;

import jakarta.persistence.LockModeType;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.QueryHint;

import static java.util.Collections.emptyMap;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.mapOfSize;

/**
 * @author Strong Liu
 */
public class QueryHintDefinition {
	private final String queryName;
	private final Map<String, Object> hintsMap;

	public QueryHintDefinition(String queryName, final QueryHint[] hints) {
		this.queryName = queryName;
		if ( isEmpty( hints ) ) {
			hintsMap = emptyMap();
		}
		else {
			final Map<String, Object> hintsMap = mapOfSize( hints.length );
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

	public Boolean getBooleanWrapper(String hintName) {
		try {
			return ConfigurationHelper.getBooleanWrapper( hintName, hintsMap, null );
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
		final Integer jakartaTimeout = getInteger( SpecHints.HINT_SPEC_QUERY_TIMEOUT );
		if ( jakartaTimeout != null ) {
			// convert milliseconds to seconds
			return (int) Math.round( jakartaTimeout.doubleValue() / 1000.0 );
		}

		final Integer javaeeTimeout = getInteger( LegacySpecHints.HINT_JAVAEE_QUERY_TIMEOUT );
		if ( javaeeTimeout != null ) {
			// convert milliseconds to seconds
			return (int) Math.round( javaeeTimeout.doubleValue() / 1000.0 );
		}

		return getInteger( HibernateHints.HINT_TIMEOUT );
	}

	public boolean getCacheability() {
		return getBoolean( HibernateHints.HINT_CACHEABLE );
	}

	public CacheMode getCacheMode() {
		final String value = getString( HibernateHints.HINT_CACHE_MODE );
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
		final String value = getString( HibernateHints.HINT_FLUSH_MODE );
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
		String hitName = HibernateHints.HINT_NATIVE_LOCK_MODE;
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
		final LockModeType lockModeType = namedQueryAnnotation.lockMode();
		final Integer lockTimeoutHint = specLockTimeout();
		final Boolean followOnLocking = getBooleanWrapper( HibernateHints.HINT_FOLLOW_ON_LOCKING );

		return determineLockOptions( lockModeType, lockTimeoutHint, followOnLocking );
	}

	private Integer specLockTimeout() {
		final Integer jakartaLockTimeout = getInteger( AvailableSettings.JAKARTA_LOCK_TIMEOUT );
		if ( jakartaLockTimeout != null ) {
			return jakartaLockTimeout;
		}

		return getInteger( AvailableSettings.JPA_LOCK_TIMEOUT );
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
