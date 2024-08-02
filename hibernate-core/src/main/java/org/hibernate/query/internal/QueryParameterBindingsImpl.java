/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.hibernate.Filter;
import org.hibernate.Incubating;
import org.hibernate.QueryParameterException;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.cache.spi.QueryKey;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.FilterImpl;
import org.hibernate.internal.util.collections.CollectionHelper;
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
	public static final QueryParameterBindings EMPTY = from( ParameterMetadataImpl.EMPTY, null );

	private final SessionFactoryImplementor sessionFactory;
	private final ParameterMetadataImplementor parameterMetadata;

	private final LinkedHashMap<QueryParameter<?>, QueryParameterBinding<?>> parameterBindingMap;
	private final HashMap<Object, QueryParameterBinding<?>> parameterBindingMapByNameOrPosition;

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

		final Set<? extends QueryParameter<?>> queryParameters = parameterMetadata.getRegistrations();
		this.parameterBindingMap = CollectionHelper.linkedMapOfSize( queryParameters.size() );
		this.parameterBindingMapByNameOrPosition = CollectionHelper.mapOfSize( queryParameters.size() );
		for ( QueryParameter<?> queryParameter : queryParameters ) {
			//noinspection unchecked
			final QueryParameterBindingImpl<Object> binding = new QueryParameterBindingImpl<>(
					(QueryParameter<Object>) queryParameter,
					sessionFactory,
					(BindableType<Object>) parameterMetadata.getInferredParameterType( queryParameter )
			);
			parameterBindingMap.put( queryParameter, binding );
		}
		for ( Map.Entry<QueryParameter<?>, QueryParameterBinding<?>> entry : parameterBindingMap.entrySet() ) {
			final QueryParameter<?> queryParameter = entry.getKey();
			if ( queryParameter.getName() != null ) {
				parameterBindingMapByNameOrPosition.put( queryParameter.getName(), entry.getValue() );
			}
			else if ( queryParameter.getPosition() != null ) {
				parameterBindingMapByNameOrPosition.put( queryParameter.getPosition(), entry.getValue() );
			}
		}
	}

	private QueryParameterBindingsImpl(QueryParameterBindingsImpl original, SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
		this.parameterMetadata = original.parameterMetadata;
		this.parameterBindingMap = CollectionHelper.linkedMapOfSize( original.parameterBindingMap.size() );
		this.parameterBindingMapByNameOrPosition = CollectionHelper.mapOfSize( original.parameterBindingMapByNameOrPosition.size() );
		for ( Map.Entry<QueryParameter<?>, QueryParameterBinding<?>> entry : original.parameterBindingMap.entrySet() ) {
			final QueryParameterBinding<?> binding = entry.getValue();
			//noinspection unchecked
			parameterBindingMap.put(
					entry.getKey(),
					new QueryParameterBindingImpl<>(
							(QueryParameter<Object>) binding.getQueryParameter(),
							sessionFactory,
							(BindableType<Object>) binding.getBindType()
					)
			);
		}
		for ( Map.Entry<QueryParameter<?>, QueryParameterBinding<?>> entry : parameterBindingMap.entrySet() ) {
			final QueryParameter<?> queryParameter = entry.getKey();
			if ( queryParameter.getName() != null ) {
				parameterBindingMapByNameOrPosition.put( queryParameter.getName(), entry.getValue() );
			}
			else if ( queryParameter.getPosition() != null ) {
				parameterBindingMapByNameOrPosition.put( queryParameter.getPosition(), entry.getValue() );
			}
		}
	}

	public QueryParameterBindingsImpl copyWithoutValues(SessionFactoryImplementor sessionFactory) {
		return new QueryParameterBindingsImpl( this, sessionFactory );
	}

	@Override
	public boolean isBound(QueryParameterImplementor<?> parameter) {
		return getBinding( parameter ).isBound();
	}

	@Override
	public <P> QueryParameterBinding<P> getBinding(QueryParameterImplementor<P> parameter) {
		final QueryParameterBinding<?> binding = parameterBindingMap.get( parameter );
		if ( binding == null ) {
			throw new IllegalArgumentException(
					"Cannot create binding for parameter reference [" + parameter + "] - reference is not a parameter of this query"
			);
		}
		//noinspection unchecked
		return (QueryParameterBinding<P>) binding;
	}

	@Override
	public <P> QueryParameterBinding<P> getBinding(int position) {
		final QueryParameterBinding<?> binding = parameterBindingMapByNameOrPosition.get( position );
		if ( binding == null ) {
			// Invoke this method to throw the exception
			parameterMetadata.getQueryParameter( position );
		}
		//noinspection unchecked
		return (QueryParameterBinding<P>) binding;
	}

	@Override
	public <P> QueryParameterBinding<P> getBinding(String name) {
		final QueryParameterBinding<?> binding = parameterBindingMapByNameOrPosition.get( name );
		if ( binding == null ) {
			// Invoke this method to throw the exception
			parameterMetadata.getQueryParameter( name );
		}
		//noinspection unchecked
		return (QueryParameterBinding<P>) binding;
	}

	@Override
	public void validate() {
		for ( Map.Entry<QueryParameter<?>, QueryParameterBinding<?>> entry : parameterBindingMap.entrySet() ) {
			if ( !entry.getValue().isBound() ) {
				final QueryParameter<?> queryParameter = entry.getKey();
				if ( queryParameter.getName() != null ) {
					throw new QueryParameterException( "No argument for named parameter ':" + queryParameter.getName() + "'" );
				}
				else {
					throw new QueryParameterException( "No argument for ordinal parameter '?" + queryParameter.getPosition() + "'" );
				}
			}
		}
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
		parameterBindingMap.forEach( action );
	}

	@Override
	public QueryKey.ParameterBindingsMemento generateQueryKeyMemento(SharedSessionContractImplementor session) {
		final MutableCacheKeyImpl mutableCacheKey = new MutableCacheKeyImpl( parameterBindingMap.size() );
		final JavaType<Object> tenantIdentifierJavaType = session.getFactory().getTenantIdentifierJavaType();
		final Object tenantId = session.getTenantIdentifierValue();
		mutableCacheKey.addValue( tenantIdentifierJavaType.getMutabilityPlan().disassemble( tenantId, session ) );
		mutableCacheKey.addHashCode( tenantId == null ? 0 : tenantIdentifierJavaType.extractHashCode( tenantId ) );

		final TypeConfiguration typeConfiguration = session.getFactory().getTypeConfiguration();
		// We know that parameters are consumed in processing order, this ensures consistency of generated cache keys
		for ( Map.Entry<QueryParameter<?>, QueryParameterBinding<?>> entry : parameterBindingMap.entrySet() ) {
			final QueryParameter<?> queryParameter = entry.getKey();
			final QueryParameterBinding<?> binding = entry.getValue();
			assert binding.isBound() : "Found unbound query parameter while generating cache key";

			final MappingModelExpressible<?> mappingType = determineMappingType(
					binding,
					queryParameter,
					typeConfiguration
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
		}
		// Note: The following loops rely on getEnabledFilters() and getParameters() to return sorted maps
		final LoadQueryInfluencers loadQueryInfluencers = session.getLoadQueryInfluencers();
		for ( Map.Entry<String, Filter> entry : loadQueryInfluencers.getEnabledFilters().entrySet() ) {
			final FilterImpl filter = (FilterImpl) entry.getValue();
			final FilterDefinition filterDefinition = filter.getFilterDefinition();
			for ( Map.Entry<String, ?> paramEntry : filter.getParameters().entrySet() ) {
				final JdbcMapping jdbcMapping = filterDefinition.getParameterJdbcMapping( paramEntry.getKey() );
				final Object paramValue = paramEntry.getValue();
				addBasicValueToCacheKey( mutableCacheKey, paramValue, jdbcMapping, session );
			}
		}

		// Finally, build the overall cache key
		return mutableCacheKey.build();
	}

	private static MappingModelExpressible<?> determineMappingType(QueryParameterBinding<?> binding, QueryParameter<?> queryParameter, TypeConfiguration typeConfiguration) {
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
