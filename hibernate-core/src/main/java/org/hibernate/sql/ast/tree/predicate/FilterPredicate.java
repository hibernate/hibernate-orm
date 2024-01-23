/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.predicate;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.FilterParamResolver;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.FilterImpl;
import org.hibernate.internal.FilterJdbcParameter;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.sql.ast.SqlAstWalker;

/**
 * Collection of {@link FilterFragmentPredicate} sub-predicates, each
 * representing one {@linkplain org.hibernate.Filter enabled filter} restriction.
 *
 * @author Steve Ebersole
 * @author Nathan Xu
 */
public class FilterPredicate implements Predicate {
	private final List<FilterFragmentPredicate> fragments = new ArrayList<>();

	private List<FilterJdbcParameter> parameters;

	public FilterPredicate() {
	}

	public void applyFragment(FilterFragmentPredicate predicate) {
		fragments.add( predicate );
	}

	public void applyFragment(String processedFragment, FilterImpl filter, List<String> parameterNames, BeanContainer beanContainer) {
		fragments.add( new FilterFragmentPredicate( processedFragment, filter, parameterNames, beanContainer ) );
	}

	public void applyParameter(FilterJdbcParameter parameter) {
		if ( parameters == null ) {
			parameters = new ArrayList<>();
		}
		parameters.add( parameter );
	}

	public List<FilterFragmentPredicate> getFragments() {
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

	public static class FilterFragmentParameter {
		private final String filterName;
		private final String parameterName;
		private final JdbcMapping valueMapping;
		private final Object value;

		public FilterFragmentParameter(String filterName, String parameterName, JdbcMapping valueMapping, Object value) {
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
		private final FilterImpl filter;
		private final String sqlFragment;
		private final List<FilterFragmentParameter> parameters;

		public FilterFragmentPredicate(String sqlFragment, FilterImpl filter, List<String> parameterNames, BeanContainer beanContainer) {
			this.filter = filter;
			this.sqlFragment = sqlFragment;

			if ( CollectionHelper.isEmpty( parameterNames ) ) {
				parameters = null;
			}
			else {
				parameters = CollectionHelper.arrayList( parameterNames.size() );
				for ( int i = 0; i < parameterNames.size(); i++ ) {
					final String paramName = parameterNames.get( i );
					final Object paramValue = retrieveParamValue(filter, paramName, beanContainer);
					final FilterDefinition filterDefinition = filter.getFilterDefinition();
					final JdbcMapping jdbcMapping = filterDefinition.getParameterJdbcMapping( paramName );

					parameters.add( new FilterFragmentParameter( filter.getName(), paramName, jdbcMapping, paramValue ) );
				}
			}
		}

		public FilterImpl getFilter() {
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

		private Object retrieveParamValue(FilterImpl filter, String paramName, BeanContainer beanContainer) {
			Class<? extends FilterParamResolver> clazz = filter.getParameterResolver( paramName );
			if (clazz.isInterface()) {
				return filter.getParameter( paramName );
			}

			FilterParamResolver filterParamResolver = null;
			if (beanContainer == null) {
				try {
					filterParamResolver = clazz.getConstructor().newInstance();
				}
				catch ( Exception e ) {
					throw new MappingException( String.format( "Could not instantiate filter param resolver [resolver=%s]", clazz.getName() ), e );
				}
			} else {
				filterParamResolver = beanContainer.getBean( clazz, new BeanContainer.LifecycleOptions() {
					@Override
					public boolean canUseCachedReferences() {
						return false;
					}

					@Override
					public boolean useJpaCompliantCreation() {
						return true;
					}
				}, FallbackBeanInstanceProducer.INSTANCE ).getBeanInstance();
			}

			return filterParamResolver.resolve();
		}
	}
}
