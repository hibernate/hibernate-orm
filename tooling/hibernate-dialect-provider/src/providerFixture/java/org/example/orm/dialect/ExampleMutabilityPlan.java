/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.io.Serializable;

import org.hibernate.SharedSessionContract;
import org.hibernate.type.descriptor.java.MutabilityPlan;

/// Immutable mutability plan supplied by the external provider fixture.
///
/// @author Steve Ebersole
public final class ExampleMutabilityPlan implements MutabilityPlan<ExampleTypeValue> {
	public static final ExampleMutabilityPlan INSTANCE = new ExampleMutabilityPlan();

	private ExampleMutabilityPlan() {
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public ExampleTypeValue deepCopy(ExampleTypeValue value) {
		return value;
	}

	@Override
	public Serializable disassemble(ExampleTypeValue value, SharedSessionContract session) {
		return value == null ? null : value.text();
	}

	@Override
	public ExampleTypeValue assemble(Serializable cached, SharedSessionContract session) {
		return cached == null ? null : new ExampleTypeValue( cached.toString() );
	}
}
