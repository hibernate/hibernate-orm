package org.hibernate.sql.exec.internal;

import java.util.Collection;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.query.QueryParameter;
import org.hibernate.type.internal.TemporalPrecisionBasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.type.BindableType;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sql.spi.ParameterOccurrence;
import org.hibernate.sql.ast.spi.query.expression.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;

import static java.util.Collections.emptyList;
import static org.hibernate.query.sql.internal.NativeQueryParameterListHelper.determineBindValueMaxCount;

/**
 * Standard implementation of JdbcParameterBindings
 *
 * @author Steve Ebersole
 */
public class JdbcParameterBindingsImpl implements JdbcParameterBindings {
	private Map<JdbcParameter, JdbcParameterBinding> bindingMap;
	private Map<QueryParameter<?>, List<JdbcParameter>> temporalCacheParameters;

	/// Keep actual converted bindings with this execution, never with the query plan.
	public void registerTemporalCacheParameters(QueryParameter<?> parameter, List<JdbcParameter> parameters) {
		if ( parameters.stream().anyMatch( jdbc -> {
			final var mapping = jdbc.getExpressionType().getSingleJdbcMapping();
			return mapping.getValueConverter() != null && TemporalPrecisionBasicType.find( mapping ) != null;
		} ) ) {
			if ( temporalCacheParameters == null ) {
				temporalCacheParameters = new IdentityHashMap<>();
			}
			temporalCacheParameters.merge( parameter, List.copyOf( parameters ), (previous, next) -> {
				final List<JdbcParameter> combined = new ArrayList<>( previous.size() + next.size() );
				combined.addAll( previous );
				combined.addAll( next );
				return List.copyOf( combined );
			} );
		}
	}

	/// Contribute the already converted relational values without invoking converters again.
	@SuppressWarnings({"rawtypes", "unchecked"})
	public boolean addTemporalParameterToCacheKey(QueryParameter<?> parameter, MutableCacheKeyBuilder key,
			SharedSessionContractImplementor session) {
		final var parameters = temporalCacheParameters == null ? null : temporalCacheParameters.get( parameter );
		if ( parameters == null ) {
			return false;
		}
		key.addValue( "temporal-query-bindings-v1" );
		key.addHashCode( "temporal-query-bindings-v1".hashCode() );
		for ( var jdbc : parameters ) {
			final var binding = getBinding( jdbc );
			final var temporal = TemporalPrecisionBasicType.find( jdbc.getExpressionType().getSingleJdbcMapping() );
			final var signature = temporal == null ? binding.getBindType().getJdbcType().getClass().getName()
					: temporal.getPrecisionPlan().getSignature();
			key.addValue( signature );
			key.addHashCode( signature.hashCode() );
			final JavaType javaType = binding.getBindType().getJdbcJavaType();
			final var value = binding.getBindValue();
			key.addValue( value == null ? null : javaType.getMutabilityPlan().disassemble( value, session ) );
			key.addHashCode( value == null ? 0 : javaType.extractHashCode( value ) );
		}
		return true;
	}

	public JdbcParameterBindingsImpl(int expectedParameterCount) {
		if ( expectedParameterCount > 0 ) {
			bindingMap = new IdentityHashMap<>( expectedParameterCount );
		}
	}

	public JdbcParameterBindingsImpl(
			QueryParameterBindings queryParameterBindings,
			List<ParameterOccurrence> parameterOccurrences,
			List<JdbcParameterBinder> jdbcParameterBinders,
			SessionFactoryImplementor factory) {
		if ( !parameterOccurrences.isEmpty() ) {
			bindingMap = new IdentityHashMap<>( parameterOccurrences.size() );

			final boolean paddingEnabled = factory.getSessionFactoryOptions().inClauseParameterPaddingEnabled();
			final int inExprLimit = factory.getJdbcServices().getDialect()
					.getParameterLimits()
					.inExpressionCountLimit();

			for ( var occurrence : parameterOccurrences ) {
				final int firstBinder = jdbcParameterBinders.size();
				final var parameter = occurrence.parameter();
				final var binding = queryParameterBindings.getBinding( parameter );

				final var jdbcMapping = jdbcMapping( factory, parameter, binding );

				final BasicValueConverter valueConverter = jdbcMapping == null ? null : jdbcMapping.getValueConverter();

				if ( binding.isMultiValued() ) {
					final var bindValues = binding.getBindValues();
					final int bindValueCount = bindValues.size();
					final int bindValueMaxCount =
							determineBindValueMaxCount( paddingEnabled, inExprLimit, bindValueCount );
					Object lastBindValue = null;
					for ( Object bindValue : bindValues ) {
						final var jdbcParameter = new JdbcParameterImpl( jdbcMapping );
						jdbcParameterBinders.add( jdbcParameter );
						lastBindValue =
								valueConverter == null
										? bindValue
										: valueConverter.toRelationalValue( bindValue );
						addBinding( jdbcParameter, new JdbcParameterBindingImpl( jdbcMapping, lastBindValue ) );
					}
					if ( bindValueMaxCount != bindValueCount ) {
						for ( int i = bindValueCount; i < bindValueMaxCount; i++ ) {
							final var jdbcParameter = new JdbcParameterImpl( jdbcMapping );
							jdbcParameterBinders.add( jdbcParameter );
							addBinding( jdbcParameter, new JdbcParameterBindingImpl( jdbcMapping, lastBindValue ) );
						}
					}

				}
				else {
					final Object bindValue = binding.getBindValue();
					final Object convertedBindValue =
							valueConverter != null && bindValue != null
									? valueConverter.toRelationalValue( bindValue )
									: bindValue;
					final var jdbcParameter = new JdbcParameterImpl( jdbcMapping );
					jdbcParameterBinders.add( jdbcParameter );
					addBinding( jdbcParameter, new JdbcParameterBindingImpl( jdbcMapping, convertedBindValue ) );
				}
				if ( factory.getSessionFactoryOptions().isQueryCacheEnabled() && valueConverter != null
						&& TemporalPrecisionBasicType.find( jdbcMapping ) != null ) {
					final List<JdbcParameter> parameters = new ArrayList<>( jdbcParameterBinders.size() - firstBinder );
					for ( int i = firstBinder; i < jdbcParameterBinders.size(); i++ ) {
						parameters.add( (JdbcParameter) jdbcParameterBinders.get( i ) );
					}
					registerTemporalCacheParameters( parameter, parameters );
				}
			}
		}
	}

	private JdbcMapping jdbcMapping(
			SessionFactoryImplementor factory,
			QueryParameterImplementor<?> param,
			QueryParameterBinding<?> binding) {
		final var type = determineParamType( param, binding );
		if ( type == null ) {
			return factory.getTypeConfiguration().getBasicTypeForJavaType( Object.class );
		}
		else if ( type instanceof BasicTypeReference<?> basicTypeReference ) {
			return factory.getTypeConfiguration().getBasicTypeRegistry()
					.resolve( basicTypeReference );
		}
		else if ( type instanceof BasicValuedMapping basicValuedMapping ) {
			return basicValuedMapping.getJdbcMapping();
		}
		else {
			throw new IllegalArgumentException( "Could not resolve NativeQuery parameter type : `" + param + "`");
		}
	}

	private BindableType<?> determineParamType(QueryParameterImplementor<?> param, QueryParameterBinding<?> binding) {
		final var type = binding.getBindType();
		return type == null ? param.getHibernateType() : type;
	}

	@Override
	public void addBinding(JdbcParameter parameter, JdbcParameterBinding binding) {
		if ( bindingMap == null ) {
			bindingMap = new IdentityHashMap<>();
		}
		bindingMap.put( parameter, binding );
	}

	@Override
	public Collection<JdbcParameterBinding> getBindings() {
		return bindingMap == null ? emptyList() : bindingMap.values();
	}

	@Override
	public JdbcParameterBinding getBinding(JdbcParameter parameter) {
		return bindingMap == null ? null : bindingMap.get( parameter );
	}

	@Override
	public void visitBindings(BiConsumer<JdbcParameter, JdbcParameterBinding> action) {
		if ( bindingMap != null ) {
			for ( var entry : bindingMap.entrySet() ) {
				action.accept( entry.getKey(), entry.getValue() );
			}
		}
	}

	public void clear() {
		if ( bindingMap != null ) {
			bindingMap.clear();
		}
	}
}
