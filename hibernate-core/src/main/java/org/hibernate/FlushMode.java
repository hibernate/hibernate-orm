/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import org.hibernate.jpa.internal.util.FlushModeTypeHelper;

import jakarta.persistence.FlushModeType;

/**
 * Represents a flushing strategy. The flush process synchronizes
 * database state with session state by detecting state changes
 * and executing SQL statements. A value of this enumeration
 * specifies <em>when</em> the flush process occurs.
 * <p>
 * For example, {@link #COMMIT} specifies that the session flushes
 * automatically when the transaction is about to commit.
 * <p>
 * This enumeration represents options which may be
 * {@linkplain Session#setHibernateFlushMode set at the session
 * level}, and competes with the JPA-defined enumeration
 * {@link jakarta.persistence.FlushModeType}. Alternatively, a
 * {@link org.hibernate.query.QueryFlushMode QueryFlushMode} may
 * be specified for a given query.
 *
 * @see Session#setHibernateFlushMode
 * @see org.hibernate.query.QueryFlushMode
 *
 * @author Gavin King
 */
public enum FlushMode {
	/**
	 * The {@link Session} is only flushed when {@link Session#flush()}
	 * is called explicitly. This mode is very efficient for read-only
	 * transactions.
	 */
	MANUAL,

	/**
	 * The {@link Session} is flushed when {@link Transaction#commit()}
	 * is called. It is never automatically flushed before query
	 * execution.
	 *
	 * @see FlushModeType#COMMIT
	 */
	COMMIT,

	/**
	 * The {@link Session} is flushed when {@link Transaction#commit()}
	 * is called, and is sometimes flushed before query execution in
	 * order to ensure that queries never return stale state. This is
	 * the default flush mode.
	 *
	 * @see FlushModeType#AUTO
	 */
	AUTO,

	/**
	 * The {@link Session} is flushed when {@link Transaction#commit()}
	 * is called and before every query. This is usually unnecessary and
	 * inefficient.
	 */
	ALWAYS;

	/**
	 * Compare this flush mode to the given flush mode.
	 *
	 * @return {@code true} if this flush mode flushes less often than
	 *         the given flush mode
	 */
	public boolean lessThan(FlushMode other) {
		return this.level() < other.level();
	}

	/**
	 * Interprets an external representation of a flush mode.
	 *
	 * @param externalName the name of a {@code FlushMode}, or of a
	 *                     {@link jakarta.persistence.FlushModeType}
	 *
	 * @return a {@code FlushMode}, or null if the argument was null
	 *
	 * @throws MappingException for an unrecognized external representation
	 */
	public static FlushMode interpretExternalSetting(String externalName) {
		return FlushModeTypeHelper.interpretExternalSetting( externalName );
	}

	private int level() {
		return switch (this) {
			case ALWAYS -> 20;
			case AUTO -> 10;
			case COMMIT -> 5;
			case MANUAL -> 0;
		};
	}

	public static FlushMode fromJpaFlushMode(FlushModeType flushModeType) {
		return FlushModeTypeHelper.getFlushMode( flushModeType );
	}

	public static FlushModeType toJpaFlushMode(FlushMode flushMode) {
		return FlushModeTypeHelper.getFlushModeType( flushMode );
	}

	public FlushModeType toJpaFlushMode() {
		return FlushModeTypeHelper.getFlushModeType( this );
	}
}
