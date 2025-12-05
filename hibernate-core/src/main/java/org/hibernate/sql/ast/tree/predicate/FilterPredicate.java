/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.predicate;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Filter;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * Collection of {@link FilterFragmentPredicate} sub-predicates, each
 * representing one {@linkplain org.hibernate.Filter enabled filter} restriction.
 *
 * @author Steve Ebersole
 * @author Nathan Xu
 */
public class FilterPredicate implements Predicate {
	private final List<FilterFragmentPredicate> fragments = new ArrayList<>();

//	private List<FilterJdbcParameter> parameters;

	public FilterPredicate() {
	}

	public void applyFragment(FilterFragmentPredicate predicate) {
		fragments.add( predicate );
	}

	public void applyFragment(String processedFragment, Filter filter, List<String> parameterNames) {
		applyFragment( new FilterFragmentPredicate( processedFragment, filter, parameterNames ) );
	}

//	public void applyParameter(FilterJdbcParameter parameter) {
//		if ( parameters == null ) {
//			parameters = new ArrayList<>();
//		}
//		parameters.add( parameter );
//	}

	public List<FilterFragmentPredicate> getFragments() {
		return fragments;
	}

//	public List<FilterJdbcParameter> getParameters() {
//		return parameters;
//	}

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

	private static List<FilterFragmentParameter> fragmentParameters(Filter filter, List<String> parameterNames) {
		if ( CollectionHelper.isEmpty( parameterNames ) ) {
			return null;
		}
		else {
			final int parameterCount = parameterNames.size();
			final List<FilterFragmentParameter> parameters = arrayList( parameterCount );
			for ( int i = 0; i < parameterCount; i++ ) {
				final String paramName = parameterNames.get( i );
				final Object paramValue = filter.getParameterValue( paramName );
				final var jdbcMapping = filter.getFilterDefinition().getParameterJdbcMapping( paramName );
				parameters.add( new FilterFragmentParameter( filter.getName(), paramName, jdbcMapping, paramValue ) );
			}
			return parameters;
		}
	}

	public static class FilterFragmentParameter {
		private final String filterName;
		private final String parameterName;
		private final JdbcMapping valueMapping;
		private final Object value;

		FilterFragmentParameter(String filterName, String parameterName, JdbcMapping valueMapping, Object value) {
			this.filterName = filterName;
			this.parameterName = parameterName;
			this.valueMapping = valueMapping;
			this.value = value;
		}

		public String getFilterName() {
			return filterName;
		}

		public String getParameterName() {
			return parameterName;
		}

		public JdbcMapping getValueMapping() {
			return valueMapping;
		}

		public Object getValue() {
			return value;
		}
	}

	public static class FilterFragmentPredicate implements Predicate {
		private final Filter filter;
		private final String sqlFragment;
		private final List<FilterFragmentParameter> parameters;

		FilterFragmentPredicate(String sqlFragment, Filter filter, List<String> parameterNames) {
			this.filter = filter;
			this.sqlFragment = sqlFragment;
			this.parameters = fragmentParameters( filter, parameterNames );
		}

		public Filter getFilter() {
			return filter;
		}

		public String getFilterName() {
			return filter.getName();
		}

		public String getSqlFragment() {
			return sqlFragment;
		}

		public List<FilterFragmentParameter> getParameters() {
			return parameters;
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
