/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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

		LockOptions lockOptions = new LockOptions( LockModeConverter.convertToLockMode( lockModeType ) );
		if ( lockTimeoutHint != null ) {
			lockOptions.setTimeOut( lockTimeoutHint );
		}

		return lockOptions;
	}

	public Map<String, Object> getHintsMap() {
		return hintsMap;
	}
}
