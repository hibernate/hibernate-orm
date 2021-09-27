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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.hibernate.Incubating;
import org.hibernate.QueryException;
import org.hibernate.QueryParameterException;
import org.hibernate.cache.spi.QueryKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypedExpressable;
import org.hibernate.type.spi.TypeConfiguration;

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
	private final boolean queryParametersValidationEnabled;

	private Map<QueryParameter<?>, QueryParameterBinding<?>> parameterBindingMap;

	/**
	 * Constructs a QueryParameterBindings based on the passed information
	 *
	 * @apiNote Calls {@link #from(ParameterMetadataImplementor,SessionFactoryImplementor,boolean)}
	 * using {@link org.hibernate.boot.spi.SessionFactoryOptions#isQueryParametersValidationEnabled}
	 * as `queryParametersValidationEnabled`
	 */
	public static QueryParameterBindingsImpl from(
			ParameterMetadataImplementor parameterMetadata,
			SessionFactoryImplementor sessionFactory) {
		return from(
				parameterMetadata,
				sessionFactory,
				sessionFactory.getSessionFactoryOptions().isQueryParametersValidationEnabled()
		);
	}

	/**
	 * Constructs a QueryParameterBindings based on the passed information
	 */
	public static QueryParameterBindingsImpl from(
			ParameterMetadataImplementor parameterMetadata,
			SessionFactoryImplementor sessionFactory,
			boolean queryParametersValidationEnabled) {
		if ( parameterMetadata == null ) {
			throw new QueryParameterException( "Query parameter metadata cannot be null" );
		}

		return new QueryParameterBindingsImpl(
				sessionFactory,
				parameterMetadata,
				queryParametersValidationEnabled
		);
	}

	private QueryParameterBindingsImpl(
			SessionFactoryImplementor sessionFactory,
			ParameterMetadataImplementor parameterMetadata,
			boolean queryParametersValidationEnabled) {
		this.sessionFactory = sessionFactory;
		this.parameterMetadata = parameterMetadata;
		this.queryParametersValidationEnabled = queryParametersValidationEnabled;

		this.parameterBindingMap = new ConcurrentHashMap<>( parameterMetadata.getParameterCount() );
	}

	@SuppressWarnings({"WeakerAccess" })
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
				parameterMetadata.getInferredParameterType( queryParameter ),
				queryParametersValidationEnabled
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
							throw new QueryException( "Named parameter not bound : " + queryParameter.getName() );
						}
						else {
							throw new QueryException( "Ordinal parameter not bound : " + queryParameter.getPosition() );
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
	public QueryKey.ParameterBindingsMemento generateQueryKeyMemento(SharedSessionContractImplementor persistenceContext) {
		final int size = parameterBindingMap.size();
		final List<Object> allBindValues = new ArrayList<>( size );
		int hashCode = 0;

		for ( QueryParameterBinding<?> binding : parameterBindingMap.values() ) {
			final MappingModelExpressable<?> mappingType = determineMappingType( binding, persistenceContext );
			assert mappingType instanceof JavaTypedExpressable;

			if ( binding.isMultiValued() ) {
				for ( Object bindValue : binding.getBindValues() ) {
					assert bindValue != null;

					final Object disassembled = mappingType.disassemble( bindValue, persistenceContext );
					allBindValues.add( disassembled );

					//noinspection unchecked
					final int valueHashCode = ( (JavaTypedExpressable<Object>) mappingType ).getExpressableJavaTypeDescriptor().extractHashCode( bindValue );

					hashCode = 31 * hashCode + valueHashCode;
				}
			}
			else {
				final Object bindValue = binding.getBindValue();

				final Object disassembled = mappingType.disassemble( bindValue, persistenceContext );
				allBindValues.add( disassembled );

				//noinspection unchecked
				final int valueHashCode = ( (JavaTypedExpressable<Object>) mappingType ).getExpressableJavaTypeDescriptor().extractHashCode( bindValue );

				hashCode = 31 * hashCode + valueHashCode;
			}
		}

		return new ParameterBindingsMementoImpl( allBindValues.toArray( new Object[0] ), hashCode );
	}

	private MappingModelExpressable<?> determineMappingType(QueryParameterBinding<?> binding, SharedSessionContractImplementor session) {
		if ( binding.getBindType() != null ) {
			if ( binding.getBindType() instanceof MappingModelExpressable ) {
				//noinspection unchecked
				return (MappingModelExpressable<Object>) binding.getBindType();
			}
		}

		if ( binding.getType() != null ) {
			return binding.getType();
		}

		final TypeConfiguration typeConfiguration = session.getFactory().getTypeConfiguration();

		if ( binding.getBindType() instanceof JavaTypedExpressable ) {
			final JavaTypedExpressable javaTypedExpressable = (JavaTypedExpressable) binding.getBindType();
			final JavaTypeDescriptor jtd = javaTypedExpressable.getExpressableJavaTypeDescriptor();
			if ( jtd.getJavaTypeClass() != null ) {
				// avoid dynamic models
				return typeConfiguration.getBasicTypeForJavaType( jtd.getJavaTypeClass() );
			}
		}

		if ( binding.isMultiValued() ) {
			final Object firstBindValue = binding.getBindValues().iterator().next();
			return typeConfiguration.getBasicTypeForJavaType( firstBindValue.getClass() );
		}
		else {
			return typeConfiguration.getBasicTypeForJavaType( binding.getBindValue().getClass() );
		}
	}


	private static class ParameterBindingsMementoImpl implements QueryKey.ParameterBindingsMemento {
		private final Object[] values;
		private final int hashCode;

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
			// Probably incorrect - comparing Object[] arrays with Arrays.equals
			return Arrays.equals( values, queryKey.values );
		}

		@Override
		public int hashCode() {
			return hashCode;
		}
	}
}
