/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.predicate;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.internal.FilterJdbcParameter;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;

/**
 * Represents a filter applied to an entity/collection.
 * <p/>
 * Note, we do not attempt to parse the filter
 *
 * @author Steve Ebersole
 * @author Nathan Xu
 */
public class FilterPredicate implements Predicate {
	private Junction fragments = new Junction();
	private List<FilterJdbcParameter> parameters;

	public FilterPredicate() {
	}

	public void applyFragment(FilterFragmentPredicate predicate) {
		fragments.add( predicate );
	}

	public void applyFragment(String sqlFragment) {
		fragments.add( new FilterFragmentPredicate( sqlFragment ) );
	}

	public void applyParameter(FilterJdbcParameter parameter) {
		if ( parameters == null ) {
			parameters = new ArrayList<>();
		}
		parameters.add( parameter );
	}

	public Junction getFragments() {
		return fragments;
	}

	public List<FilterJdbcParameter> getParameters() {
		return parameters;
	}

	@Override
	public boolean isEmpty() {
		return fragments.isEmpty();
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitFilterPredicate( this );
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return null;
	}
	public static class FilterFragmentPredicate implements Predicate {
		private final String sqlFragment;

		public FilterFragmentPredicate(String sqlFragment) {
			this.sqlFragment = sqlFragment;
		}

		public String getSqlFragment() {
			return sqlFragment;
		}

		@Override
		public void accept(SqlAstWalker sqlTreeWalker) {
			sqlTreeWalker.visitFilterFragmentPredicate( this );
		}

		@Override
		public JdbcMappingContainer getExpressionType() {
			return null;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}
	}
}
