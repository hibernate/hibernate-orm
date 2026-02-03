/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.jdbc.Expectation;

import java.util.function.Supplier;

/**
 * Enumerates strategies for checking JDBC return codes for custom SQL DML queries within the JAXB model.
 * Ultimately converted into an {@linkplain Expectation}.
 *
 * @author László Benke
 *
 * @see SQLInsert#verify()
 * @see SQLUpdate#verify()
 * @see SQLDelete#verify()
 * @see SQLDeleteAll#verify()
 *
 * @see Expectation
 */
public enum ResultCheckStyle {
	/**
	 * No return code checking. Might mean that no checks are required, or that
	 * failure is indicated by a {@link java.sql.SQLException} being thrown, for
	 * example, by a {@linkplain java.sql.CallableStatement stored procedure} which
	 * performs explicit checks.
	 *
	 * @see org.hibernate.jdbc.Expectation.None
	 */
	NONE,
	/**
	 * Row count checking. A row count is an integer value returned by
	 * {@link java.sql.PreparedStatement#executeUpdate()} or
	 * {@link java.sql.Statement#executeBatch()}. The row count is checked
	 * against an expected value. For example, the expected row count for
	 * an {@code INSERT} statement is always 1.
	 *
	 * @see org.hibernate.jdbc.Expectation.RowCount
	 */
	COUNT,
	/**
	 * Essentially identical to {@link #COUNT} except that the row count is
	 * obtained via an output parameter of a {@linkplain java.sql.CallableStatement
	 * stored procedure}.
	 * <p>
	 * Statement batching is disabled when {@code PARAM} is selected.
	 *
	 * @see org.hibernate.jdbc.Expectation.OutParameter
	 */
	PARAM;


	public String externalName() {
		return switch ( this ) {
			case NONE -> "none";
			case COUNT -> "rowcount";
			case PARAM -> "param";
		};
	}

	public static @Nullable ResultCheckStyle fromExternalName(String name) {
		for ( ResultCheckStyle style : values() ) {
			if ( style.externalName().equalsIgnoreCase(name) ) {
				return style;
			}
		}
		return null;
	}

	public Class<? extends Expectation> expectationClass() {
		return switch ( this ) {
			case NONE -> Expectation.None.class;
			case COUNT -> Expectation.RowCount.class;
			case PARAM -> Expectation.OutParameter.class;
		};
	}

	public Supplier<? extends Expectation> expectationSupplier() {
		return switch ( this ) {
			case NONE -> Expectation.None::new;
			case COUNT -> Expectation.RowCount::new;
			case PARAM -> Expectation.OutParameter::new;
		};
	}
}
