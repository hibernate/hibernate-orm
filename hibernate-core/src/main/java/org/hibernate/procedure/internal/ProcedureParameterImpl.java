/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.procedure.internal;

import java.util.Objects;
import javax.persistence.ParameterMode;

import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.query.AbstractQueryParameter;
import org.hibernate.query.procedure.spi.ProcedureParameterImplementor;

/**
 * @author Steve Ebersole
 */
public class ProcedureParameterImpl<T> extends AbstractQueryParameter<T> implements ProcedureParameterImplementor<T> {
	private final String name;
	private final Integer position;

	private final ParameterMode mode;

	private final Class<T> javaType;


	public ProcedureParameterImpl(
			String name,
			ParameterMode mode,
			Class<T> javaType,
			AllowableParameterType<T> hibernateType) {
		super( false, hibernateType );
		this.name = name;
		this.position = null;
		this.mode = mode;
		this.javaType = javaType;
	}

	public ProcedureParameterImpl(
			Integer position,
			ParameterMode mode,
			Class<T> javaType,
			AllowableParameterType<T> hibernateType) {
		super( false, hibernateType );
		this.name = null;
		this.position = position;
		this.mode = mode;
		this.javaType = javaType;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Integer getPosition() {
		return position;
	}

	@Override
	public ParameterMode getMode() {
		return mode;
	}

	@Override
	public Class<T> getParameterType() {
		return javaType;
	}

	public NamedCallableQueryMemento.ParameterMemento toMemento() {
		return session -> {
			if ( getName() != null ) {
				//noinspection unchecked
				return new ProcedureParameterImpl(
						getName(),
						getMode(),
						javaType,
						getHibernateType()
				);
			}
			else {
				//noinspection unchecked
				return new ProcedureParameterImpl(
						getPosition(),
						getMode(),
						javaType,
						getHibernateType()
				);
			}
		};
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ProcedureParameterImpl<?> that = (ProcedureParameterImpl<?>) o;
		return Objects.equals( name, that.name ) &&
				Objects.equals( position, that.position ) &&
				mode == that.mode;
	}

	@Override
	public int hashCode() {
		return Objects.hash( name, position, mode );
	}
}
