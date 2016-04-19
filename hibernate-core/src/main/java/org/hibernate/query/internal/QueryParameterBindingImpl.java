/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import javax.persistence.TemporalType;

import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class QueryParameterBindingImpl<T> implements QueryParameterBinding<T> {
	private Type bindType;
	private T bindValue;

	public QueryParameterBindingImpl(Type type) {
		this.bindType = type;
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
		if ( value == null ) {
			throw new IllegalArgumentException( "Cannot bind null to query parameter" );
		}
		this.bindValue = value;
	}

	@Override
	public void setBindValue(T value, Type clarifiedType) {
		setBindValue( value );
		this.bindType = clarifiedType;
	}

	@Override
	public void setBindValue(T value, TemporalType clarifiedTemporalType) {
		setBindValue( value );
		this.bindType = BindingTypeHelper.INSTANCE.determineTypeForTemporalType( clarifiedTemporalType, bindType, value );
	}
}
