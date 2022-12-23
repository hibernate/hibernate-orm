/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import java.util.Set;

import org.hibernate.internal.FilterJdbcParameter;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.query.spi.QueryOptions;

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
	 */
	Set<FilterJdbcParameter> getFilterJdbcParameters();

	/**
	 * Signals that the SQL depends on the parameter bindings e.g. due to the need for inlining
	 * of parameter values or multiValued parameters.
	 */
	boolean dependsOnParameterBindings();

	boolean isCompatibleWith(JdbcParameterBindings jdbcParameterBindings, QueryOptions queryOptions);

	default void bindFilterJdbcParameters(JdbcParameterBindings jdbcParameterBindings) {
		if ( CollectionHelper.isNotEmpty( getFilterJdbcParameters() ) ) {
			for ( FilterJdbcParameter filterJdbcParameter : getFilterJdbcParameters() ) {
				jdbcParameterBindings.addBinding( filterJdbcParameter.getParameter(), filterJdbcParameter.getBinding() );
			}
		}
	}
}
