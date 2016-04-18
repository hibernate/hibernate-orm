/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import javax.persistence.TemporalType;

import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.sql.gen.NotYetImplementedException;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class QueryParameterBindingImpl implements QueryParameterBinding {
	private Type bindType;
	private Object bindValue;

	public QueryParameterBindingImpl() {
	}

	@Override
	public Object getBindValue() {
		return bindValue;
	}

	@Override
	public Type getBindType() {
		return bindType;
	}

	@Override
	public void setBindValue(Object value) {
		if ( value == null ) {
			throw new IllegalArgumentException( "Cannot bind null to query parameter" );
		}
		this.bindValue = value;
	}

	@Override
	public void setBindValue(Object value, Type clarifiedType) {
		setBindValue( value );
		this.bindType = clarifiedType;
	}

	@Override
	public void setBindValue(Object value, TemporalType clarifiedTemporalType) {
		throw new NotYetImplementedException( "swapping types based on TemporalType not yet implemented" );
	}
}
