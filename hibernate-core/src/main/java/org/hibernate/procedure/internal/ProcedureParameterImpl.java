/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import javax.persistence.ParameterMode;

import org.hibernate.query.internal.AbstractQueryParameter;
import org.hibernate.procedure.ProcedureParameter;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public class ProcedureParameterImpl<T> extends AbstractQueryParameter<T> implements ProcedureParameter<T> {
	private final String name;
	private final Integer position;

	private final ParameterMode mode;

	private final Class<T> javaType;


	public ProcedureParameterImpl(String name, ParameterMode mode, Class<T> javaType, Type hibernateType, boolean passNulls) {
		super( false, passNulls, hibernateType );
		this.name = name;
		this.position = null;
		this.mode = mode;
		this.javaType = javaType;
	}

	public ProcedureParameterImpl(Integer position, ParameterMode mode, Class<T> javaType, Type hibernateType, boolean passNulls) {
		super( false, passNulls, hibernateType );
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

}
