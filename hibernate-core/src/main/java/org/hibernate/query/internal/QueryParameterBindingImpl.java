/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.Collection;
import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.BindableType;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindingValidator;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.expression.NullSqmExpressible;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.JavaTypeHelper;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

/**
 * The standard implementation of {@link QueryParameterBinding}.
 *
 * @author Steve Ebersole
 */
public class QueryParameterBindingImpl<T> implements QueryParameterBinding<T>, JavaType.CoercionContext {
	private final QueryParameter<T> queryParameter;
	private final SessionFactoryImplementor sessionFactory;

	private boolean isBound;
	private boolean isMultiValued;

	private BindableType<? super T> bindType;
	private MappingModelExpressible<T> type;
	private TemporalType explicitTemporalPrecision;

	private Object bindValue;
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
			BindableType<T> bindType) {
		this.queryParameter = queryParameter;
		this.sessionFactory = sessionFactory;
		this.bindType = bindType;
	}

	@Override
	public BindableType<? super T> getBindType() {
		return bindType;
	}

	@Override
	public TemporalType getExplicitTemporalPrecision() {
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

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// single-valued binding support

	@Override
	public T getBindValue() {
		if ( isMultiValued ) {
			throw new IllegalStateException( "Binding is multi-valued; illegal call to #getBindValue" );
		}

		//TODO: I believe this cast is unsound due to coercion
		return (T) bindValue;
	}

	@Override
	public void setBindValue(T value, boolean resolveJdbcTypeIfNecessary) {
		if ( handleAsMultiValue( value, null ) ) {
			return;
		}

		final Object coerced;
		if ( ! sessionFactory.getSessionFactoryOptions().getJpaCompliance().isLoadByIdComplianceEnabled() ) {
			try {
				if ( canValueBeCoerced( bindType) ) {
					coerced = coerce( value, bindType );
				}
				else if ( canValueBeCoerced( queryParameter.getHibernateType() ) ) {
					coerced = coerce( value, queryParameter.getHibernateType() );
				}
				else {
					coerced = value;
				}
			}
			catch (HibernateException ce) {
				throw new IllegalArgumentException(
						String.format(
								"Parameter value [%s] did not match expected type [%s]",
								value,
								bindType
						),
						ce
				);
			}
		}
		else {
			coerced = value;
		}

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

	private void bindNull(boolean resolveJdbcTypeIfNecessary) {
		isBound = true;
		bindValue = null;
		if ( resolveJdbcTypeIfNecessary && bindType == null ) {
			bindType = getTypeConfiguration().getBasicTypeRegistry().getRegisteredType( "null" );
		}
	}

	private Object coerce(T value, BindableType<? super T> parameterType) {
		if ( value == null ) {
			return null;
		}

		final SqmExpressible<? super T> sqmExpressible = parameterType.resolveExpressible( sessionFactory );
		assert sqmExpressible != null;
		return sqmExpressible.getExpressibleJavaType().coerce( value, this );
	}

	private boolean handleAsMultiValue(T value, BindableType<T> bindableType) {
		if ( queryParameter.allowsMultiValuedBinding()
				&& value instanceof Collection
				&& ( bindableType == null
					? !isRegisteredAsBasicType( value.getClass() )
					: !bindableType.getBindableJavaType().isInstance( value ) ) ) {
			//noinspection unchecked
			setBindValues( (Collection<T>) value );
			return true;
		}
		else {
			return false;
		}
	}

	private boolean isRegisteredAsBasicType(Class<?> valueClass) {
		return getTypeConfiguration().getBasicTypeForJavaType( valueClass ) != null;
	}

	private void bindValue(Object value) {
		isBound = true;
		bindValue = value;
		if ( canBindValueBeSet( value, bindType ) ) {
			bindType = sessionFactory.getMappingMetamodel().resolveParameterBindType( value );
		}
	}

	@Override
	public void setBindValue(T value, BindableType<T> clarifiedType) {
		if ( handleAsMultiValue( value, clarifiedType ) ) {
			return;
		}

		if ( clarifiedType != null ) {
			bindType = clarifiedType;
		}

		final Object coerced;
		if ( canValueBeCoerced( bindType ) ) {
			coerced = coerce( value, bindType );
		}
		else if ( canValueBeCoerced( queryParameter.getHibernateType() ) ) {
			coerced = coerce( value, queryParameter.getHibernateType() );
		}
		else {
			coerced = value;
		}

		validate( coerced, clarifiedType );

		bindValue( coerced );
	}

	@Override
	public void setBindValue(T value, TemporalType temporalTypePrecision) {
		if ( handleAsMultiValue( value, null ) ) {
			return;
		}

		if ( bindType == null ) {
			bindType = queryParameter.getHibernateType();
		}

		final Object coerced;
		if ( ! sessionFactory.getSessionFactoryOptions().getJpaCompliance().isLoadByIdComplianceEnabled() ) {
			if (canValueBeCoerced( bindType ) ) {
				try {
					coerced = coerce( value, bindType );
				}
				catch (HibernateException ex) {
					throw new IllegalArgumentException(
							String.format(
									"Parameter value [%s] did not match expected type [%s (%s)]",
									value,
									bindType,
									temporalTypePrecision == null ? "n/a" : temporalTypePrecision.name()
							),
							ex
					);
				}
			}
			else if ( canValueBeCoerced( queryParameter.getHibernateType() ) ) {
				coerced = coerce( value, queryParameter.getHibernateType() );
			}
			else {
				coerced = value;
			}
		}
		else {
			coerced = value;
		}

		validate( coerced, temporalTypePrecision );

		bindValue( coerced );
		setExplicitTemporalPrecision( temporalTypePrecision );
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
	public void setBindValues(Collection<? extends T> values) {
		if ( !queryParameter.allowsMultiValuedBinding() ) {
			throw new IllegalArgumentException(
					"Illegal attempt to bind a collection value to a single-valued parameter"
			);
		}

		this.isBound = true;
		this.isMultiValued = true;

		this.bindValue = null;
		this.bindValues = values;

		final Iterator<? extends T> iterator = values.iterator();
		T value = null;
		while ( value == null && iterator.hasNext() ) {
			value = iterator.next();
		}

		if ( canBindValueBeSet( value, bindType ) ) {
			bindType = sessionFactory.getMappingMetamodel().resolveParameterBindType( value );
		}
	}

	@Override
	public void setBindValues(Collection<? extends T> values, BindableType<T> clarifiedType) {
		if ( clarifiedType != null ) {
			this.bindType = clarifiedType;
		}
		setBindValues( values );
	}

	@Override
	public void setBindValues(
			Collection<? extends T> values,
			TemporalType temporalTypePrecision,
			TypeConfiguration typeConfiguration) {
		setBindValues( values );
		setExplicitTemporalPrecision( temporalTypePrecision );
	}

	private void setExplicitTemporalPrecision(TemporalType precision) {
		explicitTemporalPrecision = precision;
		if ( bindType == null || JavaTypeHelper.isTemporal( determineJavaType( bindType ) ) ) {
			bindType = BindingTypeHelper.INSTANCE.resolveTemporalPrecision( precision, bindType, sessionFactory );
		}
	}

	private JavaType<? super T> determineJavaType(BindableType<? super T> bindType) {
		final SqmExpressible<? super T> sqmExpressible = bindType.resolveExpressible( sessionFactory );
		assert sqmExpressible != null;
		return sqmExpressible.getExpressibleJavaType();
	}

	@Override
	public MappingModelExpressible<T> getType() {
		return type;
	}

	@Override @SuppressWarnings("unchecked")
	public boolean setType(MappingModelExpressible<T> type) {
		this.type = type;
		// If the bind type is undetermined or the given type is a model part, then we try to apply a new bind type
		if ( bindType == null || bindType.getBindableJavaType() == Object.class || type instanceof ModelPart ) {
			if ( type instanceof BindableType<?> ) {
				final boolean changed = bindType != null && type != bindType;
				bindType = (BindableType<T>) type;
				return changed;
			}
			else if ( type instanceof BasicValuedMapping ) {
				final JdbcMapping jdbcMapping = ( (BasicValuedMapping) type ).getJdbcMapping();
				if ( jdbcMapping instanceof BindableType<?> ) {
					final boolean changed = bindType != null && jdbcMapping != bindType;
					bindType = (BindableType<T>) jdbcMapping;
					return changed;
				}
			}
		}
		return false;
	}

	private void validate(Object value) {
		QueryParameterBindingValidator.INSTANCE.validate( getBindType(), value, sessionFactory );
	}

	private void validate(Object value, BindableType<?> clarifiedType) {
		QueryParameterBindingValidator.INSTANCE.validate( clarifiedType, value, sessionFactory );
	}

	private void validate(Object value, TemporalType clarifiedTemporalType) {
		QueryParameterBindingValidator.INSTANCE.validate( getBindType(), value, clarifiedTemporalType, sessionFactory );
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return sessionFactory.getTypeConfiguration();
	}

	private static boolean canValueBeCoerced(BindableType<?> bindType) {
		return bindType != null && !( bindType instanceof NullSqmExpressible );
	}

	private static boolean canBindValueBeSet(Object value, BindableType<?> bindType) {
		return value != null && ( bindType == null || bindType instanceof NullSqmExpressible );
	}
}
