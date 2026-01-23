/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.persistence.FlushModeType;
import org.hibernate.FlushMode;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.util.Locale;

/**
 * Enumerates the possible flush modes for execution of a
 * {@link org.hibernate.query.Query}. An explicitly-specified
 * {@linkplain Query#setQueryFlushMode(QueryFlushMode)
 * query-level flush mode} overrides the current
 * {@linkplain org.hibernate.Session#getHibernateFlushMode()
 * flush mode of the session}.
 *
 * @since 7.0
 *
 * @see CommonQueryContract#setQueryFlushMode(QueryFlushMode)
 * @see org.hibernate.annotations.NamedQuery#flush
 * @see org.hibernate.annotations.NamedNativeQuery#flush
 *
 * @author Gavin King
 */
public enum QueryFlushMode {
	/**
	 * Flush before executing the query.
	 */
	FLUSH,
	/**
	 * Do not flush before executing the query.
	 */
	NO_FLUSH,
	/**
	 * Let the owning {@linkplain org.hibernate.Session session}
	 * decide whether to flush, depending on its current
	 * {@link org.hibernate.FlushMode}.
	 *
	 * @see org.hibernate.Session#getFlushMode()
	 */
	DEFAULT;

	public static QueryFlushMode fromHint(Object value) {
		if ( value == null ) {
			return QueryFlushMode.DEFAULT;
		}
		else if ( value instanceof QueryFlushMode qfm ) {
			return qfm;
		}
		else if ( value instanceof FlushMode fm ) {
			return QueryFlushMode.fromHibernateMode( fm );
		}
		else if ( value instanceof FlushModeType fmt ) {
			return QueryFlushMode.fromJpaMode( fmt );
		}
		else {
			return QueryFlushMode.interpretHint( value.toString() );
		}
	}

	public FlushMode toHibernateFlushMode(SharedSessionContractImplementor session) {
		if ( this == DEFAULT ) {
			return session.getHibernateFlushMode();
		}
		else if ( this == FLUSH ) {
			return FlushMode.AUTO;
		}
		else {
			return FlushMode.MANUAL;
		}
	}

	public FlushModeType toJpaFlushMode() {
		if ( this == DEFAULT ) {
			return FlushModeType.AUTO;
		}
		else {
			return FlushModeType.COMMIT;
		}
	}

	public static QueryFlushMode fromJpaMode(FlushModeType jpaMode) {
		if ( jpaMode == null ) {
			return DEFAULT;
		}
		else if ( jpaMode == FlushModeType.COMMIT ) {
			return NO_FLUSH;
		}
		else {
			return FLUSH;
		}
	}

	public static QueryFlushMode fromHibernateMode(FlushMode hibernateMode) {
		if ( hibernateMode == null ) {
			return DEFAULT;
		}
		else if ( hibernateMode == FlushMode.COMMIT || hibernateMode == FlushMode.MANUAL ) {
			return NO_FLUSH;
		}
		else {
			return FLUSH;
		}
	}

	public static QueryFlushMode interpretHint(String value) {
		if ( value == null ) {
			return DEFAULT;
		}

		final String capitalizedValue = value.toUpperCase( Locale.ROOT );
		try {
			return QueryFlushMode.valueOf( capitalizedValue );
		}
		catch (IllegalArgumentException iae) {
			// fall through
		}

		try {
			return fromHibernateMode( FlushMode.valueOf( capitalizedValue ) );
		}
		catch (IllegalArgumentException iae) {
			// fall through
		}

		try {
			return fromJpaMode( FlushModeType.valueOf( capitalizedValue ) );
		}
		catch (IllegalArgumentException iae) {
			// fall through
		}

		throw new IllegalArgumentException( "Incoming value could not be interpreted as a QueryFlushMode : " + value );
	}
}
