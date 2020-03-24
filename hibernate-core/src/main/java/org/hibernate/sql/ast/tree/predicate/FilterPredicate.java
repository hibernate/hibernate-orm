/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.predicate;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.internal.FilterHelper;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;

/**
 * Represents a filter applied to an entity/collection.
 * <p/>
 * Note, we do not attempt to parse the filter
 *
 * @author Steve Ebersole
 */
public class FilterPredicate implements Predicate {
	private final String filterFragment;
	private final List<JdbcParameter> jdbcParameters;
	private final List<FilterHelper.TypedValue> jdbcParameterTypedValues;

	public FilterPredicate(String filterFragment, List<FilterHelper.TypedValue> jdbcParameterTypedValues) {
		this.filterFragment = filterFragment;
		jdbcParameters = new ArrayList<>( jdbcParameterTypedValues.size() );
		this.jdbcParameterTypedValues = jdbcParameterTypedValues;
		for (int i = 0; i < jdbcParameterTypedValues.size(); i++) {
			jdbcParameters.add( new JdbcParameterImpl( null ) );
		}
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitFilterPredicate( this );
	}

	public String getFilterFragment() {
		return filterFragment;
	}

	public List<JdbcParameter> getJdbcParameters() {
		return jdbcParameters;
	}

	public List<FilterHelper.TypedValue> getJdbcParameterTypedValues() {
		return jdbcParameterTypedValues;
	}
}
