/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import javax.persistence.TemporalType;

import org.hibernate.procedure.ParameterBind;

/**
 * Implementation of the {@link ParameterBind} contract.
 *
 * @author Steve Ebersole
 */
public class ParameterBindImpl<T> implements ParameterBind<T> {
	private final T value;
	private final TemporalType explicitTemporalType;

	ParameterBindImpl(T value) {
		this( value, null );
	}

	ParameterBindImpl(T value, TemporalType explicitTemporalType) {
		this.value = value;
		this.explicitTemporalType = explicitTemporalType;
	}

	@Override
	public T getValue() {
		return value;
	}

	@Override
	public TemporalType getExplicitTemporalType() {
		return explicitTemporalType;
	}
}
