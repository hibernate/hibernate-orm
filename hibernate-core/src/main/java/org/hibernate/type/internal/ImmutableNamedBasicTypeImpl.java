/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.internal;

import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * @author Christian Beikov
 */
public class ImmutableNamedBasicTypeImpl<J> extends NamedBasicTypeImpl<J> {

	public ImmutableNamedBasicTypeImpl(
			JavaType<J> jtd,
			JdbcType std,
			String name) {
		super( jtd, std, name );
	}

	@Override
	protected MutabilityPlan<J> getMutabilityPlan() {
		return ImmutableMutabilityPlan.instance();
	}
}
