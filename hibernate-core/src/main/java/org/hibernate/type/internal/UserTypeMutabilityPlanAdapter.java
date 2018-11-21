/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import java.io.Serializable;

import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.usertype.UserType;

/**
 * @author Steve Ebersole
 */
public class UserTypeMutabilityPlanAdapter<T> implements MutabilityPlan<T> {
	private final UserType userType;

	public UserTypeMutabilityPlanAdapter(UserType userType) {
		this.userType = userType;
	}

	@Override
	public boolean isMutable() {
		return userType.isMutable();
	}

	@Override
	@SuppressWarnings("unchecked")
	public T deepCopy(T value) {
		return (T) userType.deepCopy( value );
	}

	@Override
	public Serializable disassemble(T value) {
		return userType.disassemble( value );
	}

	@Override
	@SuppressWarnings("unchecked")
	public T assemble(Serializable cached) {
		return (T) userType.assemble( cached, null );
	}
}
