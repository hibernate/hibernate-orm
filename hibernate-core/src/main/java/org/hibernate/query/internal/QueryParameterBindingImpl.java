/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.Collection;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.type.BindableType;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindingTypeResolver;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

import static org.hibernate.type.descriptor.java.JavaTypeHelper.isTemporal;
import static org.hibernate.type.internal.BindingTypeHelper.resolveTemporalPrecision;

/**
 * The standard implementation of {@link QueryParameterBinding}.
 *
 * @author Steve Ebersole
 */
public class QueryParameterBindingImpl<T> implements QueryParameterBinding<T> {
	private final QueryParameter<T> queryParameter;
	private final SessionFactoryImplementor sessionFactory;

	private boolean isBound;
	private boolean isMultiValued;

	private @Nullable BindableType<T> bindType;
	private @Nullable MappingModelExpressible<T> type;
	private @Nullable @SuppressWarnings("deprecation") TemporalType explicitTemporalPrecision;

	private T bindValue;
	private Collection<? extends T> bindValues;

	/**
	 * Used by {@link org.hibernate.procedure.ProcedureCall}
	 */
	protected QueryParameterBindingImpl(
			QueryParameter<T> queryParameter,
			SessionFactoryImplementor sessionFactory) {
		this( queryParameter, sessionFactory, queryParameter.getHibernateType() );
	}

	/**
	 * Used by Query (SQM) and NativeQuery
	 */
	public QueryParameterBindingImpl(
			QueryParameter<T> queryParameter,
			SessionFactoryImplementor sessionFactory,
			@Nullable BindableType<T> bindType) {
		this.queryParameter = queryParameter;
		this.sessionFactory = sessionFactory;
		this.bindType = bindType;
	}

	private QueryParameterBindingTypeResolver getParameterBindingTypeResolver() {
		return sessionFactory.getMappingMetamodel();
	}

	public TypeConfiguration getTypeConfiguration() {
		return sessionFactory.getTypeConfiguration();
	}

	@Override
	public @Nullable BindableType<T> getBindType() {
		return bindType;
	}

	@Override
	public @Nullable @SuppressWarnings("deprecation") TemporalType getExplicitTemporalPrecision() {
		return explicitTemporalPrecision;
	}

	@Override
	public boolean isBound() {
		return isBound;
	}

	@Override
	public boolean isMultiValued() {
		return isMultiValued;
	}

	@Override
	public QueryParameter<T> getQueryParameter() {
		return queryParameter;
	}

	private NodeBuilder getCriteriaBuilder() {
		return sessionFactory.getQueryEngine().getCriteriaBuilder();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// single-valued binding support

	@Override
	public T getBindValue() {
		if ( isMultiValued ) {
			throw new IllegalStateException( "Binding is multi-valued; illegal call to #getBindValue" );
		}
		return bindValue;
	}

	@Override
	public void setBindValue(Object value, boolean resolveJdbcTypeIfNecessary) {
		if ( !handleAsMultiValue( value, null ) ) {
			final Object coerced = coerce( value );
			validate( coerced );
			if ( value == null ) {
				// needed when setting a null value to the parameter of a native SQL query
				// TODO: this does not look like a very disciplined way to handle this
				bindNull( resolveJdbcTypeIfNecessary );
			}
			else {
				bindValue( coerced );
			}
		}
	}

	@Override
	public void setBindValue(Object value, @Nullable BindableType<T> clarifiedType) {
		if ( !handleAsMultiValue( value, clarifiedType ) ) {
			if ( clarifiedType != null ) {
				bindType = clarifiedType;
			}

			final Object coerced = coerce( value );
			validate( coerced );
			bindValue( coerced );
		}
	}

	@Override
	public void setBindValue(Object value, @SuppressWarnings("deprecation") TemporalType temporalTypePrecision) {
		if ( !handleAsMultiValue( value, null ) ) {
			if ( bindType == null ) {
				bindType = queryParameter.getHibernateType();
			}

			final Object coerced = coerce( value );
			validate( coerced );
			bindValue( coerced );
			setExplicitTemporalPrecision( temporalTypePrecision );
		}
	}

	private void bindValue(Object value) {
		if ( canBindValueBeSet( value, bindType ) ) {
			bindType = (BindableType<T>) (BindableType)
					getParameterBindingTypeResolver()
							.resolveParameterBindType( value );
		}
		bindValue = cast( value );
		isBound = true;
	}

	private void bindNull(boolean resolveJdbcTypeIfNecessary) {
		isBound = true;
		bindValue = null;
		if ( resolveJdbcTypeIfNecessary && bindType == null ) {
			final var nullType =
					getTypeConfiguration().getBasicTypeRegistry()
							.getRegisteredType( "null" );
			//noinspection unchecked
			bindType = (BindableType<T>) nullType;
		}
	}

	private boolean handleAsMultiValue(Object value, @Nullable BindableType<T> bindableType) {
		if ( queryParameter.allowsMultiValuedBinding()
			&& value instanceof Collection
			&& !( bindableType == null
				? isRegisteredAsBasicType( value.getClass() )
				: bindableType.getJavaType().isInstance( value ) ) ) {
			setBindValues( (Collection<?>) value );
			return true;
		}
		else {
			return false;
		}
	}

	private boolean isRegisteredAsBasicType(Class<?> valueClass) {
		return getTypeConfiguration().getBasicTypeForJavaType( valueClass ) != null;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// multi-valued binding support

	@Override
	public Collection<? extends T> getBindValues() {
		if ( !isMultiValued ) {
			throw new IllegalStateException( "Binding is not multi-valued; illegal call to #getBindValues" );
		}
		return bindValues;
	}

	@Override
	public void setBindValues(Collection<?> values) {
		if ( !queryParameter.allowsMultiValuedBinding() ) {
			throw new IllegalArgumentException(
					"Illegal attempt to bind a collection value to a single-valued parameter"
			);
		}

		final var coerced = values.stream().map( this::coerce ).toList();
		values.forEach( this::validate );

		isBound = true;
		isMultiValued = true;

		bindValue = null;
		bindValues = coerced.stream().map( this::cast ).toList();

		final T value = firstNonNull( bindValues );
		if ( canBindValueBeSet( value, bindType ) ) {
			bindType = (BindableType<T>) (BindableType)
					getParameterBindingTypeResolver()
							.resolveParameterBindType( value );
		}
	}

	private static <T> @Nullable T firstNonNull(Collection<? extends T> values) {
		final var iterator = values.iterator();
		T value = null;
		while ( value == null && iterator.hasNext() ) {
			value = iterator.next();
		}
		return value;
	}

	@Override
	public void setBindValues(Collection<?> values, BindableType<T> clarifiedType) {
		if ( clarifiedType != null ) {
			bindType = clarifiedType;
		}
		setBindValues( values );
	}

	@Override
	public void setBindValues(
			Collection<?> values,
			@SuppressWarnings("deprecation") TemporalType temporalTypePrecision,
			TypeConfiguration typeConfiguration) {
		setBindValues( values );
		setExplicitTemporalPrecision( temporalTypePrecision );
	}

	private void setExplicitTemporalPrecision(@SuppressWarnings("deprecation") TemporalType precision) {
		explicitTemporalPrecision = precision;
		if ( bindType == null || isTemporal( determineJavaType( bindType ) ) ) {
			bindType = resolveTemporalPrecision( precision, bindType, getCriteriaBuilder() );
		}
	}

	private JavaType<? super T> determineJavaType(BindableType<? super T> bindType) {
		return getCriteriaBuilder().resolveExpressible( bindType ).getExpressibleJavaType();
	}

	@Override
	public @Nullable MappingModelExpressible<T> getType() {
		return type;
	}

	@Override @SuppressWarnings("unchecked")
	public boolean setType(@Nullable MappingModelExpressible<T> type) {
		this.type = type;
		// If the bind type is undetermined or the given type is a model part, then we try to apply a new bind type
		if ( bindType == null || bindType.getJavaType() == Object.class || type instanceof ModelPart ) {
			if ( type instanceof BindableType<?> ) {
				final boolean changed = bindType != null && type != bindType;
				bindType = (BindableType<T>) type;
				return changed;
			}
			else if ( type instanceof BasicValuedMapping basicValuedMapping ) {
				final var jdbcMapping = basicValuedMapping.getJdbcMapping();
				if ( jdbcMapping instanceof BindableType<?> ) {
					final boolean changed = bindType != null && jdbcMapping != bindType;
					bindType = (BindableType<T>) jdbcMapping;
					return changed;
				}
			}
		}
		return false;
	}

	private T cast(Object value) {
		final var bindableType = getCriteriaBuilder().resolveExpressible( bindType );
		return bindableType == null
				? (T) value // YOLO
				: QueryArguments.cast( value, bindableType.getExpressibleJavaType() );
	}

	private void validate(Object value) {
		QueryParameterBindingValidator.validate( getBindType(), value, sessionFactory );
	}

	private Object coerce(Object value) {
		try {
			if ( canValueBeCoerced( bindType ) ) {
				return coerce( value, bindType );
			}
			else if ( canValueBeCoerced( queryParameter.getHibernateType() ) ) {
				return coerce( value, queryParameter.getHibernateType() );
			}
			else {
				return value;
			}
		}
		catch (HibernateException ex) {
			throw new IllegalArgumentException(
					String.format(
							"Parameter value [%s] did not match expected type [%s]",
							value,
							bindType
					),
					ex
			);
		}
	}

	private Object coerce(Object value, BindableType<T> parameterType) {
		return value == null ? null
				: getCriteriaBuilder().resolveExpressible( parameterType )
						.getExpressibleJavaType().coerce( value );
	}

	private static boolean canValueBeCoerced(BindableType<?> bindType) {
		return bindType != null;
	}

	private static boolean canBindValueBeSet(Object value, BindableType<?> bindType) {
		return value != null && bindType == null;
	}
}
