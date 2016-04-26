/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.procedure.internal;

import javax.persistence.TemporalType;

import org.hibernate.query.procedure.spi.ProcedureParameterBindingImplementor;
import org.hibernate.query.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class ProcedureParameterBindingImpl<T> implements ProcedureParameterBindingImplementor<T> {
	private final ProcedureParameterImplementor<T> parameter;

	public ProcedureParameterBindingImpl(ProcedureParameterImplementor<T> parameter) {
		this.parameter = parameter;
	}

	@Override
	public boolean isBound() {
		return parameter.getNativeParameterRegistration().getBind() != null;
	}

	@Override
	public void setBindValue(T value) {
		parameter.getNativeParameterRegistration().bindValue( value );
	}

	@Override
	public void setBindValue(T value, Type clarifiedType) {
		parameter.getNativeParameterRegistration().setHibernateType( clarifiedType );
		parameter.getNativeParameterRegistration().bindValue( value );
	}

	@Override
	public void setBindValue(T value, TemporalType clarifiedTemporalType) {
		parameter.getNativeParameterRegistration().bindValue( value, clarifiedTemporalType );
	}

	@Override
	public T getBindValue() {
		return parameter.getNativeParameterRegistration().getBind().getValue();
	}

	@Override
	public Type getBindType() {
		return parameter.getNativeParameterRegistration().getHibernateType();
	}
}
