/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.process.internal;

import java.io.Serializable;

import org.hibernate.SharedSessionContract;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.usertype.UserType;

/**
 * @author Steve Ebersole
 */
public class UserTypeMutabilityPlanAdapter<T> implements MutabilityPlan<T> {
	private final UserType<T> userType;

	public UserTypeMutabilityPlanAdapter(UserType<T> userType) {
		this.userType = userType;
	}

	@Override
	public boolean isMutable() {
		return userType.isMutable();
	}

	@Override
	public T deepCopy(T value) {
		return userType.deepCopy( value );
	}

	@Override
	public Serializable disassemble(T value, SharedSessionContract session) {
		return userType.disassemble( value );
	}

	@Override
	public T assemble(Serializable cached, SharedSessionContract session) {
		return userType.assemble( cached, null );
	}
}
