/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.hibernate.Incubating;
import org.hibernate.QueryParameterException;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.cache.spi.QueryKey;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.FilterImpl;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.BindableType;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.JavaTypedExpressible;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.engine.internal.CacheHelper.addBasicValueToCacheKey;

/**
 * Manages the group of QueryParameterBinding for a particular query.
 *
 * @author Steve Ebersole
 * @author Chris Cranford
 */
@Incubating
public class QueryParameterBindingsImpl implements QueryParameterBindings {
	private final SessionFactoryImplementor sessionFactory;
	private final ParameterMetadataImplementor parameterMetadata;

	private Map<QueryParameter<?>, QueryParameterBinding<?>> parameterBindingMap;

	/**
	 * Constructs a QueryParameterBindings based on the passed information
	 */
	public static QueryParameterBindingsImpl from(
			ParameterMetadataImplementor parameterMetadata,
			SessionFactoryImplementor sessionFactory) {
		if ( parameterMetadata == null ) {
			throw new QueryParameterException( "Query parameter metadata cannot be null" );
		}

		return new QueryParameterBindingsImpl(
				sessionFactory,
				parameterMetadata
		);
	}

	private QueryParameterBindingsImpl(
			SessionFactoryImplementor sessionFactory,
			ParameterMetadataImplementor parameterMetadata) {
		this.sessionFactory = sessionFactory;
		this.parameterMetadata = parameterMetadata;

		this.parameterBindingMap = new ConcurrentHashMap<>( parameterMetadata.getParameterCount() );
	}

	protected <T> QueryParameterBinding<T> makeBinding(QueryParameterImplementor<T> queryParameter) {
		if ( parameterBindingMap == null ) {
			parameterBindingMap = new IdentityHashMap<>();
		}
		else {
			assert ! parameterBindingMap.containsKey( queryParameter );
		}

		if ( ! parameterMetadata.containsReference( queryParameter ) ) {
			throw new IllegalArgumentException(
					"Cannot create binding for parameter reference [" + queryParameter + "] - reference is not a parameter of this query"
			);
		}

		final QueryParameterBinding<T> binding = new QueryParameterBindingImpl<>(
				queryParameter,
				sessionFactory,
				parameterMetadata.getInferredParameterType( queryParameter )
		);
		parameterBindingMap.put( queryParameter, binding );

		return binding;
	}

	@Override
	public boolean isBound(QueryParameterImplementor<?> parameter) {
		return getBinding( parameter ).isBound();
	}

	@Override
	public <P> QueryParameterBinding<P> getBinding(QueryParameterImplementor<P> parameter) {
		if ( parameterBindingMap == null ) {
			return makeBinding( parameter );
		}

		QueryParameterBinding<?> binding = parameterBindingMap.get( parameter );

		if ( binding == null ) {
			binding = makeBinding( parameter );
		}

		//noinspection unchecked
		return (QueryParameterBinding<P>)  binding;
	}

	@Override
	public <P> QueryParameterBinding<P> getBinding(int position) {
		//noinspection unchecked
		return (QueryParameterBinding<P>) getBinding( parameterMetadata.getQueryParameter( position ) );
	}

	@Override
	public <P> QueryParameterBinding<P> getBinding(String name) {
		//noinspection unchecked
		return (QueryParameterBinding<P>) getBinding( parameterMetadata.getQueryParameter( name ) );
	}

	@Override
	public void validate() {
		parameterMetadata.visitRegistrations(
				queryParameter -> {
					if ( ! parameterBindingMap.containsKey( queryParameter ) ) {
						if ( queryParameter.getName() != null ) {
							throw new QueryParameterException( "No argument for named parameter ':" + queryParameter.getName() + "'" );
						}
						else {
							throw new QueryParameterException( "No argument for ordinal parameter '?" + queryParameter.getPosition() + "'" );
						}
					}
				}
		);
	}

	@Override
	public boolean hasAnyMultiValuedBindings() {
		for ( QueryParameterBinding<?> binding : parameterBindingMap.values() ) {
			if ( binding.isMultiValued() ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void visitBindings(@SuppressWarnings("rawtypes") BiConsumer action) {
		parameterMetadata.visitRegistrations(
				queryParameterImplementor -> {
					//noinspection unchecked
					action.accept( queryParameterImplementor, parameterBindingMap.get( queryParameterImplementor ) );
				}
		);
	}

	@Override
	public QueryKey.ParameterBindingsMemento generateQueryKeyMemento(SharedSessionContractImplementor session) {
		final MutableCacheKeyImpl mutableCacheKey = new MutableCacheKeyImpl( parameterBindingMap.size() );
		final JavaType<Object> tenantIdentifierJavaType = session.getFactory().getTenantIdentifierJavaType();
		final Object tenantId = session.getTenantIdentifierValue();
		mutableCacheKey.addValue( tenantIdentifierJavaType.getMutabilityPlan().disassemble( tenantId, session ) );
		mutableCacheKey.addHashCode( tenantId == null ? 0 : tenantIdentifierJavaType.extractHashCode( tenantId ) );
		// We know that parameters are consumed in processing order, this ensures consistency of generated cache keys
		parameterMetadata.visitParameters( queryParameter -> {
			final QueryParameterBinding<?> binding = parameterBindingMap.get( queryParameter );
			assert binding != null : "Found unbound query parameter while generating cache key";

			final MappingModelExpressible<?> mappingType = determineMappingType(
					binding,
					queryParameter,
					session
			);
			if ( binding.isMultiValued() ) {
				for ( Object bindValue : binding.getBindValues() ) {
					assert bindValue != null;
					mappingType.addToCacheKey( mutableCacheKey, bindValue, session );
				}
			}
			else {
				final Object bindValue = binding.getBindValue();
				mappingType.addToCacheKey( mutableCacheKey, bindValue, session );
			}
		} );
		// Add any enabled filter parameter values to the cache key, sorting by filter name and parameter name
		final LoadQueryInfluencers loadQueryInfluencers = session.getLoadQueryInfluencers();
		loadQueryInfluencers.getEnabledFilterNames().stream().sorted().forEach( filterName -> {
			final FilterImpl filter = (FilterImpl) loadQueryInfluencers.getEnabledFilter( filterName );
			final FilterDefinition filterDefinition = filter.getFilterDefinition();
			filterDefinition.getParameterNames().stream().sorted().forEach( paramName -> {
				final Object paramValue = filter.getParameter( paramName );
				final JdbcMapping jdbcMapping = filterDefinition.getParameterJdbcMapping( paramName );
				addBasicValueToCacheKey( mutableCacheKey, paramValue, jdbcMapping, session );
			} );
		} );
		// Finally, build the overall cache key
		return mutableCacheKey.build();
	}

	private MappingModelExpressible<?> determineMappingType(final QueryParameterBinding<?> binding, final QueryParameter<?> queryParameter, final SharedSessionContractImplementor session) {
		final BindableType<?> bindType = binding.getBindType();
		if ( bindType != null ) {
			if ( bindType instanceof MappingModelExpressible ) {
				//noinspection unchecked
				return (MappingModelExpressible<Object>) bindType;
			}
		}

		final MappingModelExpressible<?> type = binding.getType();
		if ( type != null ) {
			return type;
		}

		final TypeConfiguration typeConfiguration = session.getFactory().getTypeConfiguration();

		if ( bindType instanceof JavaTypedExpressible) {
			final JavaTypedExpressible<?> javaTypedExpressible = (JavaTypedExpressible<?>) bindType;
			final JavaType<?> jtd = javaTypedExpressible.getExpressibleJavaType();
			if ( jtd.getJavaTypeClass() != null ) {
				// avoid dynamic models
				return typeConfiguration.getBasicTypeForJavaType( jtd.getJavaTypeClass() );
			}
		}

		if ( binding.isMultiValued() ) {
			final Iterator<?> iterator = binding.getBindValues().iterator();
			Object firstNonNullBindValue = null;
			if ( iterator.hasNext() ) {
				firstNonNullBindValue = iterator.next();
			}
			if ( firstNonNullBindValue != null ) {
				return typeConfiguration.getBasicTypeForJavaType( firstNonNullBindValue.getClass() );
			}
		}
		else if ( binding.getBindValue() != null ) {
			return typeConfiguration.getBasicTypeForJavaType( binding.getBindValue().getClass() );
		}

		if ( bindType == null ) {
			if ( queryParameter.getName() != null ) {
				throw new QueryParameterException( "Could not determine mapping type for named parameter ':" + queryParameter.getName() + "'" );
			}
			else {
				throw new QueryParameterException( "Could not determine mapping type for ordinal parameter '?" + queryParameter.getPosition() + "'" );
			}
		}
		return typeConfiguration.getBasicTypeForJavaType( bindType.getBindableJavaType() );
	}

	private static class MutableCacheKeyImpl implements MutableCacheKeyBuilder {

		final List<Object> values;
		int hashCode;

		public MutableCacheKeyImpl(int parameterBindingMapSize) {
			values = new ArrayList<>( parameterBindingMapSize );
		}

		@Override
		public void addValue(Object value) {
			values.add( value );
		}

		@Override
		public void addHashCode(int hashCode) {
			this.hashCode = 37 * this.hashCode + hashCode;
		}

		@Override
		public QueryKey.ParameterBindingsMemento build() {
			return new ParameterBindingsMementoImpl( values.toArray( new Object[0] ), hashCode );
		}
	}

	private static class ParameterBindingsMementoImpl implements QueryKey.ParameterBindingsMemento {
		final Object[] values;
		final int hashCode;

		private ParameterBindingsMementoImpl(Object[] values, int hashCode) {
			this.values = values;
			this.hashCode = hashCode;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			ParameterBindingsMementoImpl queryKey = (ParameterBindingsMementoImpl) o;

			if ( hashCode != queryKey.hashCode ) {
				return false;
			}
			return Arrays.deepEquals( values, queryKey.values );
		}

		@Override
		public int hashCode() {
			return hashCode;
		}
	}
}
