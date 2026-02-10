/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.QueryArgumentException;
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
		if ( handleAsMultiValue( value, bindType ) ) {
			setBindValues( (Collection<?>) value );
		}
		else {
			final Object coerced = coerce( value );
			validate( coerced );
			if ( value == null ) {
				// needed when setting a null value to the parameter of a native SQL query
				// TODO: this does not look like a very disciplined way to handle this
				bindNull( resolveJdbcTypeIfNecessary );
			}
			else {
				initBindType( value );
				bindSingleValue( coerced );
			}
		}
	}

	@Override
	public void setBindValue(
			Object value,
			@SuppressWarnings("deprecation")
			TemporalType temporalTypePrecision) {
		if ( handleAsMultiValue( value, bindType ) ) {
			setBindValues( (Collection<?>) value, temporalTypePrecision );
		}
		else {
			final Object coerced = coerce( value );
			validate( coerced );
			initBindType( value );
			bindSingleValue( coerced );
			setExplicitTemporalPrecision( temporalTypePrecision );
		}
	}

	@Override
	public <A> void setBindValue(A value, @Nullable BindableType<A> clarifiedType) {
		// don't coerce, because value is already of the clarified type
		validate( value );
		clarifyType( value, clarifiedType );
		bindSingleValue( value );
	}

	private void initBindType(Object value) {
		if ( bindType == null ) {
			@SuppressWarnings("unchecked")
			// If there is no bindType set, then this is effectively a
			// parameter of the top type. At least arguably safe cast.
			final var self = (QueryParameterBindingImpl<Object>) this;
			//noinspection UnnecessaryLocalVariable (needed to make javac happy)
			final var valueType =
					getParameterBindingTypeResolver()
							.resolveParameterBindType( value );
			self.bindType = valueType;
		}
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

	private boolean handleAsMultiValue(Object value, @Nullable BindableType<?> bindableType) {
		return queryParameter.allowsMultiValuedBinding()
			&& value instanceof Collection
			&& !validInstance( value, bindableType );
	}

	private void bindSingleValue(Object value) {
		bindValue = cast( value );
		bindValues = null;
		isMultiValued = false;
		isBound = true;
	}

	private boolean validInstance(Object value, @Nullable BindableType<?> bindableType) {
		return bindableType == null
				? isRegisteredAsBasicType( value.getClass() )
				: bindableType.getJavaType().isInstance( value );
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
		assertMultivalued();
		final var coerced = values.stream().map( this::coerce ).toList();
		coerced.forEach( this::validate );
		initBindType( firstNonNull( values ) );
		bindMultipleValues( coerced );
	}

	@Override
	public void setBindValues(
			Collection<?> values,
			@SuppressWarnings("deprecation") TemporalType temporalTypePrecision) {
		setBindValues( values );
		setExplicitTemporalPrecision( temporalTypePrecision );
	}

	@Override
	public <V> void setBindValues(Collection<? extends V> values, BindableType<V> clarifiedType) {
		assertMultivalued();
		// don't coerce, because value is already of the clarified type
		values.forEach( this::validate );
		clarifyType( values, clarifiedType );
		bindMultipleValues( values );
	}

	private void bindMultipleValues(Collection<?> coerced) {
		final List<T> list = new ArrayList<>();
		for ( var value : coerced ) {
			list.add( cast( value ) );
		}
		bindValues = list;
		bindValue = null;
		isMultiValued = true;
		isBound = true;
	}

	private void assertMultivalued() {
		if ( !queryParameter.allowsMultiValuedBinding() ) {
			throw new IllegalArgumentException(
					"Illegal attempt to bind a collection value to a single-valued parameter"
			);
		}
	}

	private void setExplicitTemporalPrecision(@SuppressWarnings("deprecation") TemporalType precision) {
		explicitTemporalPrecision = precision;
		if ( bindType == null || isTemporal( determineJavaType( bindType ) ) ) {
			bindType = resolveTemporalPrecision( precision, bindType, getCriteriaBuilder() );
		}
	}

	private JavaType<T> determineJavaType(BindableType<T> bindType) {
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

	private <V> void clarifyType(Object valueOrValues, BindableType<V> clarifiedType) {
		if ( clarifiedType != null ) {
			checkClarifiedType( clarifiedType, valueOrValues );
			@SuppressWarnings("unchecked") // safe
			final var newType = (BindableType<T>) clarifiedType;
			bindType = newType;
		}
	}

	private <A> void checkClarifiedType(
			@NonNull BindableType<A> clarifiedType,
			Object valueOrValues) {
		final var parameterType = queryParameter.getParameterType();
		if ( parameterType != null ) {
			final var clarifiedJavaType = clarifiedType.getJavaType();
			if ( !parameterType.isAssignableFrom( clarifiedJavaType ) ) {
				throw new QueryArgumentException(
						"Given type is incompatible with parameter type",
						parameterType, clarifiedJavaType, valueOrValues
				);
			}
		}
		else {
			assert queryParameter.getHibernateType() == null;
		}
	}

	private T cast(Object value) {
		if ( value == null ) {
			return null;
		}
		else {
			final var bindableType =
					getCriteriaBuilder()
							.resolveExpressible( bindType );
			if ( bindableType != null ) {
				return QueryArguments.cast( value,
						bindableType.getExpressibleJavaType() );
			}
			else if ( bindType != null ) {
				return bindType.getJavaType().cast( value );
			}
			else {
				// no typing information, but in this
				// case we can view this as T = Object
				// noinspection unchecked
				return (T) value;
			}
		}
	}

	private void validate(Object value) {
		QueryParameterBindingValidator.validate( queryParameter, bindType, value, sessionFactory );
	}

	private Object coerce(Object value) {
		try {
			if ( bindType != null ) {
				return coerce( value, bindType );
			}
//			else if ( queryParameter.getHibernateType() != null ) {
//				return coerce( value, queryParameter.getHibernateType() );
//			}
			else {
				return value;
			}
		}
		catch (HibernateException ex) {
			throw new QueryArgumentException( "Argument to query parameter has an incompatible type: " + ex.getMessage(),
					queryParameter.getParameterType(), value );
		}
	}

	private Object coerce(Object value, BindableType<T> parameterType) {
		return value == null ? null
				: getCriteriaBuilder().resolveExpressible( parameterType )
						.getExpressibleJavaType().coerce( value );
	}

	private static <T> @Nullable T firstNonNull(Collection<? extends T> values) {
		final var iterator = values.iterator();
		T value = null;
		while ( value == null && iterator.hasNext() ) {
			value = iterator.next();
		}
		return value;
	}

}
