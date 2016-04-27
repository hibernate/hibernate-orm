/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.Collection;
import javax.persistence.TemporalType;

import org.hibernate.query.spi.QueryParameterListBinding;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class QueryParameterListBindingImpl<T> implements QueryParameterListBinding<T> {
	private Collection<T> bindValues;
	private Type bindType;

	public QueryParameterListBindingImpl(Type type) {
		this.bindType = type;
	}

	@Override
	public void setBindValues(Collection<T> bindValues) {
		if ( bindValues == null ) {
			throw new IllegalArgumentException( "Collection must be not null!" );
		}
		this.bindValues = bindValues;
	}

	@Override
	public void setBindValues(Collection<T> values, Type clarifiedType) {
		setBindValues( values );
		this.bindType = clarifiedType;
	}

	@Override
	public void setBindValues(Collection<T> values, TemporalType clarifiedTemporalType) {
		setBindValues( values );
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
}
