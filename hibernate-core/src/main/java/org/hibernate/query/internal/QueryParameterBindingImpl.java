/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import javax.persistence.TemporalType;

import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindingTypeResolver;
import org.hibernate.query.spi.QueryParameterBindingValidator;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class QueryParameterBindingImpl<T> implements QueryParameterBinding<T> {
	private final QueryParameterBindingTypeResolver typeResolver;
	private final boolean isBindingValidationRequired;

	private boolean isBound;

	private Type bindType;
	private T bindValue;

	public QueryParameterBindingImpl(
			Type type,
			QueryParameterBindingTypeResolver typeResolver,
			boolean isBindingValidationRequired) {
		this.bindType = type;
		this.typeResolver = typeResolver;
		this.isBindingValidationRequired = isBindingValidationRequired;
	}

	@Override
	public boolean isBound() {
		return isBound;
	}

	@Override
	public T getBindValue() {
		return bindValue;
	}

	@Override
	public Type getBindType() {
		return bindType;
	}

	@Override
	public void setBindValue(T value) {
		if ( isBindingValidationRequired ) {
			validate( value );
		}
		bindValue( value );
	}

	@Override
	public void setBindValue(T value, Type clarifiedType) {
		if ( isBindingValidationRequired ) {
			validate( value, clarifiedType );
		}
		if ( clarifiedType != null ) {
			this.bindType = clarifiedType;
		}
		bindValue( value );
	}

	@Override
	public void setBindValue(T value, TemporalType clarifiedTemporalType) {
		if ( isBindingValidationRequired ) {
			validate( value, clarifiedTemporalType );
		}
		bindValue( value );
		this.bindType = BindingTypeHelper.INSTANCE.determineTypeForTemporalType( clarifiedTemporalType, bindType, value );
	}

	private void bindValue(T value) {
		this.isBound = true;
		this.bindValue = value;

		if ( bindType == null ) {
			this.bindType = typeResolver.resolveParameterBindType( value );
		}
	}

	private void validate(T value) {
		QueryParameterBindingValidator.INSTANCE.validate( getBindType(), value );
	}

	private void validate(T value, Type clarifiedType) {
		QueryParameterBindingValidator.INSTANCE.validate( clarifiedType, value );
	}

	private void validate(T value, TemporalType clarifiedTemporalType) {
		QueryParameterBindingValidator.INSTANCE.validate( getBindType(), value, clarifiedTemporalType );
	}
}
