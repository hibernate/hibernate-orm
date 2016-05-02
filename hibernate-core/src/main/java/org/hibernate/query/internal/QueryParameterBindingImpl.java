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
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class QueryParameterBindingImpl<T> implements QueryParameterBinding<T> {
	private final QueryParameterBindingTypeResolver typeResolver;

	private boolean isBound;

	private Type bindType;
	private T bindValue;

	public QueryParameterBindingImpl(Type type, QueryParameterBindingTypeResolver typeResolver) {
		this.bindType = type;
		this.typeResolver = typeResolver;
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
		this.isBound = true;
		this.bindValue = value;

		if ( bindType == null ) {
			this.bindType = typeResolver.resolveParameterBindType( value );
		}
	}

	@Override
	public void setBindValue(T value, Type clarifiedType) {
		setBindValue( value );
		if ( clarifiedType != null ) {
			this.bindType = clarifiedType;
		}
	}

	@Override
	public void setBindValue(T value, TemporalType clarifiedTemporalType) {
		setBindValue( value );
		this.bindType = BindingTypeHelper.INSTANCE.determineTypeForTemporalType( clarifiedTemporalType, bindType, value );
	}
}
