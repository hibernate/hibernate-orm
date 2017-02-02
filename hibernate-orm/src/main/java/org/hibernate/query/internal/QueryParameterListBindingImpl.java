/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.Collection;
import javax.persistence.TemporalType;

import org.hibernate.query.spi.QueryParameterBindingValidator;
import org.hibernate.query.spi.QueryParameterListBinding;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class QueryParameterListBindingImpl<T> implements QueryParameterListBinding<T> {
	private final boolean isBindingValidationRequired;

	private Collection<T> bindValues;
	private Type bindType;

	public QueryParameterListBindingImpl(Type type, boolean isBindingValidationRequired) {
		this.bindType = type;
		this.isBindingValidationRequired = isBindingValidationRequired;
	}

	@Override
	public void setBindValues(Collection<T> bindValues) {
		if ( isBindingValidationRequired ) {
			validate( bindValues );
		}
		bindValue( bindValues );
	}

	@Override
	public void setBindValues(Collection<T> values, Type clarifiedType) {
		if ( isBindingValidationRequired ) {
			validate( bindValues, clarifiedType );
		}
		bindValue( values );
		this.bindType = clarifiedType;
	}

	@Override
	public void setBindValues(Collection<T> values, TemporalType clarifiedTemporalType) {
		if ( isBindingValidationRequired ) {
			validate( values, clarifiedTemporalType );
		}
		bindValue( values );
		final Object anElement = values.isEmpty() ? null : values.iterator().next();
		this.bindType = BindingTypeHelper.INSTANCE.determineTypeForTemporalType( clarifiedTemporalType, bindType, anElement );
	}

	@Override
	public Collection<T> getBindValues() {
		return bindValues;
	}

	@Override
	public Type getBindType() {
		return bindType;
	}

	private void bindValue(Collection<T> bindValues) {
		if ( bindValues == null ) {
			throw new IllegalArgumentException( "Collection must be not null!" );
		}
		this.bindValues = bindValues;
	}

	private void validate(Collection<T> value) {
		QueryParameterBindingValidator.INSTANCE.validate( getBindType(), value );
	}

	private void validate(Collection<T> value, Type clarifiedType) {
		QueryParameterBindingValidator.INSTANCE.validate( clarifiedType, value );
	}

	private void validate(Collection<T> value, TemporalType clarifiedTemporalType) {
		QueryParameterBindingValidator.INSTANCE.validate( getBindType(), value, clarifiedTemporalType );
	}
}
