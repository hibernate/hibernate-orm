/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.group;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.model.TableMapping;

/**
 * Descriptor for details about a {@link PreparedStatement}
 *
 * @author Steve Ebersole
 */
@Incubating
public interface PreparedStatementDetails {
	/**
	 * The name of the mutating table
	 */
	TableMapping getMutatingTableDetails();

	/**
	 * The SQL used to mutate the table
	 */
	String getSqlString();

	/**
	 * The {@link PreparedStatement} generated from the SQL.  May return null.
	 *
	 * @see #resolveStatement()
	 */
	PreparedStatement getStatement();

	/**
	 * The {@link PreparedStatement} generated from the SQL.
	 * <p>
	 * Unlike {@link #getStatement()}, this method will attempt to create the PreparedStatement
	 */
	PreparedStatement resolveStatement();

	/**
	 * The expectation used to validate the outcome of the execution
	 */
	Expectation getExpectation();

	/**
	 * Whether the statement is callable
	 */
	default boolean isCallable() {
		return getStatement() instanceof CallableStatement;
	}

	void releaseStatement(SharedSessionContractImplementor session);

	default boolean toRelease(){
		return false;
	}
}
