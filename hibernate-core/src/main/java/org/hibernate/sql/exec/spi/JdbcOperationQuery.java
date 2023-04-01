/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.hibernate.internal.FilterJdbcParameter;
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
	 * Any parameters to apply for filters
	 *
	 * @see org.hibernate.annotations.Filter
	 *
	 * @deprecated No longer used.
	 */
	@Deprecated(since = "6.2")
	default Set<FilterJdbcParameter> getFilterJdbcParameters() {
		return Collections.emptySet();
	}

	/**
	 * Signals that the SQL depends on the parameter bindings e.g. due to the need for inlining
	 * of parameter values or multiValued parameters.
	 */
	boolean dependsOnParameterBindings();

	/**
	 * The parameters which were inlined into the query as literals.
	 */
	Map<JdbcParameter, JdbcParameterBinding> getAppliedParameters();

	boolean isCompatibleWith(JdbcParameterBindings jdbcParameterBindings, QueryOptions queryOptions);
}
