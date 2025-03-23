/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import java.util.Map;
import java.util.Set;

import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;

/**
 * Unifying contract for any SQL statement we want to execute via JDBC.
 *
 * @author Steve Ebersole
 */
public interface JdbcOperationQuery extends JdbcOperation {

	/**
	 * The names of tables this operation refers to
	 */
	Set<String> getAffectedTableNames();

	/**
	 * Signals that the SQL depends on the parameter bindings e.g. due to the need for inlining
	 * of parameter values or multiValued parameters.
	 */
	boolean dependsOnParameterBindings();

	/**
	 * The parameters which were inlined into the query as literals.
	 *
	 * @deprecated No longer called
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	Map<JdbcParameter, JdbcParameterBinding> getAppliedParameters();

	boolean isCompatibleWith(JdbcParameterBindings jdbcParameterBindings, QueryOptions queryOptions);
}
