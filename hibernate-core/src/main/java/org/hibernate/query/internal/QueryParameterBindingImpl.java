/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.Collection;
import java.util.Iterator;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.query.AllowableParameterType;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindingValidator;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.type.descriptor.java.CoercionException;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.TemporalJavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

/**
 * The standard Hibernate QueryParameterBinding implementation
 *
 * @author Steve Ebersole
 */
public class QueryParameterBindingImpl<T> implements QueryParameterBinding<T>, JavaType.CoercionContext {
	private final QueryParameter<T> queryParameter;
	private final SessionFactoryImplementor sessionFactory;
	private final boolean isBindingValidationRequired;

	private boolean isBound;
	private boolean isMultiValued;

	private AllowableParameterType<T> bindType;
	private MappingModelExpressable<T> type;
	private TemporalType explicitTemporalPrecision;

	private T bindValue;
	private Collection<? extends T> bindValues;

	/**
	 * Used by {@link org.hibernate.procedure.ProcedureCall}
	 */
	protected QueryParameterBindingImpl(
			QueryParameter<T> queryParameter,
			SessionFactoryImplementor sessionFactory,
			boolean isBindingValidationRequired) {
		this.queryParameter = queryParameter;
		this.sessionFactory = sessionFactory;
		this.isBindingValidationRequired = isBindingValidationRequired;
		this.bindType = queryParameter.getHibernateType();
	}

	/**
	 * Used by Query (SQM) and NativeQuery
	 */
	public QueryParameterBindingImpl(
			QueryParameter<T> queryParameter,
			SessionFactoryImplementor sessionFactory,
			AllowableParameterType<T> bindType,
			boolean isBindingValidationRequired) {
		this.queryParameter = queryParameter;
		this.sessionFactory = sessionFactory;
		this.isBindingValidationRequired = isBindingValidationRequired;
		this.bindType = bindType;
	}

	@Override
	public AllowableParameterType<T> getBindType() {
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
	public void setBindValue(T value, boolean resolveJdbcTypeIfNecessary) {
		if ( handleAsMultiValue( value ) ) {
			return;
		}

		if ( ! getTypeConfiguration().getSessionFactory().getJpaMetamodel().getJpaCompliance().isLoadByIdComplianceEnabled() ) {
			try {
				if ( bindType != null ) {
					value = coerce( value, bindType );
				}
				else if ( queryParameter.getHibernateType() != null ) {
					value = coerce( value, queryParameter.getHibernateType() );
				}
			}
			catch (CoercionException ce) {
				throw new IllegalArgumentException(
						String.format(
								"Parameter value [%s] did not match expected type [%s ]",
								value,
								bindType
						),
						ce
				);
			}
		}

		if ( isBindingValidationRequired ) {
			validate( value );
		}

		if ( resolveJdbcTypeIfNecessary && bindType == null && value == null ) {
			bindType = getTypeConfiguration().getBasicTypeRegistry().getRegisteredType( "null" );
		}
		bindValue( value );
	}

	private T coerce(T value, AllowableParameterType<T> parameterType) {
		if ( value == null ) {
			return null;
		}

		final SqmExpressable<T> sqmExpressable = parameterType.resolveExpressable( sessionFactory );
		assert sqmExpressable != null;

		return sqmExpressable.getExpressableJavaTypeDescriptor().coerce( value, this );
	}

	private boolean handleAsMultiValue(T value) {
		if ( ! queryParameter.allowsMultiValuedBinding() ) {
			return false;
		}

		if ( value == null ) {
			return false;
		}

		if ( value instanceof Collection && !isRegisteredAsBasicType( value.getClass() ) ) {
			//noinspection unchecked
			setBindValues( (Collection<T>) value );
			return true;
		}

		return false;
	}

	private boolean isRegisteredAsBasicType(Class<?> valueClass) {
		return getTypeConfiguration().getBasicTypeForJavaType( valueClass ) != null;
	}

	private void bindValue(T value) {
		this.isBound = true;
		this.bindValue = value;

		if ( bindType == null ) {
			if ( value != null ) {
				this.bindType = sessionFactory.resolveParameterBindType( value );
			}
		}
	}

	@Override
	public void setBindValue(T value, AllowableParameterType<T> clarifiedType) {
		if ( handleAsMultiValue( value ) ) {
			return;
		}

		if ( clarifiedType != null ) {
			this.bindType = clarifiedType;
		}

		if ( bindType != null ) {
			value = coerce( value, bindType );
		}
		else if ( queryParameter.getHibernateType() != null ) {
			value = coerce( value, queryParameter.getHibernateType() );
		}

		if ( isBindingValidationRequired ) {
			validate( value, clarifiedType );
		}

		bindValue( value );
	}

	@Override
	public void setBindValue(T value, TemporalType temporalTypePrecision) {
		if ( handleAsMultiValue( value ) ) {
			return;
		}

		if ( bindType == null ) {
			bindType = queryParameter.getHibernateType();
		}

		if ( ! getTypeConfiguration().getSessionFactory().getJpaMetamodel().getJpaCompliance().isLoadByIdComplianceEnabled() ) {
			if ( bindType != null ) {
				try {
					value = coerce( value, bindType );
				}
				catch (CoercionException ex) {
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
			else if ( queryParameter.getHibernateType() != null ) {
				value = coerce( value, queryParameter.getHibernateType() );
			}
		}

		if ( isBindingValidationRequired ) {
			validate( value, temporalTypePrecision );
		}

		bindValue( value );
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
		this.isBound = true;
		this.isMultiValued = true;

		this.bindValue = null;
		this.bindValues = values;

		final Iterator<? extends T> iterator = values.iterator();
		T value = null;
		while ( value == null && iterator.hasNext() ) {
			value = iterator.next();
		}

		if ( bindType == null && value != null ) {
			this.bindType = sessionFactory.resolveParameterBindType( value );
		}

	}

	@Override
	public void setBindValues(Collection<? extends T> values, AllowableParameterType<T> clarifiedType) {
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

	private void setExplicitTemporalPrecision(TemporalType temporalTypePrecision) {
		if ( bindType == null || determineJavaType( bindType ) instanceof TemporalJavaTypeDescriptor<?> ) {
			this.bindType = BindingTypeHelper.INSTANCE.resolveTemporalPrecision(
					temporalTypePrecision,
					bindType,
					sessionFactory
			);
		}

		this.explicitTemporalPrecision = temporalTypePrecision;
	}

	private JavaType<T> determineJavaType(AllowableParameterType<T> bindType) {
		final SqmExpressable<T> sqmExpressable = bindType.resolveExpressable( sessionFactory );
		assert sqmExpressable != null;

		return sqmExpressable.getExpressableJavaTypeDescriptor();
	}

	@Override
	public MappingModelExpressable<T> getType() {
		return type;
	}

	@Override @SuppressWarnings("unchecked")
	public boolean setType(MappingModelExpressable<T> type) {
		this.type = type;
		if ( bindType == null || bindType.getBindableJavaType() == Object.class ) {
			if ( type instanceof AllowableParameterType<?> ) {
				final boolean changed = bindType != null && type != bindType;
				this.bindType = (AllowableParameterType<T>) type;
				return changed;
			}
			else if ( type instanceof BasicValuedMapping ) {
				final JdbcMapping jdbcMapping = ( (BasicValuedMapping) type ).getJdbcMapping();
				if ( jdbcMapping instanceof AllowableParameterType<?> ) {
					final boolean changed = bindType != null && jdbcMapping != bindType;
					this.bindType = (AllowableParameterType<T>) jdbcMapping;
					return changed;
				}
			}
		}
		return false;
	}

	private void validate(T value) {
		QueryParameterBindingValidator.INSTANCE.validate( getBindType(), value, sessionFactory );
	}

	private void validate(T value, AllowableParameterType<?> clarifiedType) {
		QueryParameterBindingValidator.INSTANCE.validate( clarifiedType, value, sessionFactory );
	}

	private void validate(T value, TemporalType clarifiedTemporalType) {
		QueryParameterBindingValidator.INSTANCE.validate( getBindType(), value, clarifiedTemporalType, sessionFactory );
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return sessionFactory.getTypeConfiguration();
	}
}
