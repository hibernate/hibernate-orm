/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;

/**
 * @author Christian Beikov
 */
public class ImmutableNamedBasicTypeImpl<J> extends NamedBasicTypeImpl<J> {

	public ImmutableNamedBasicTypeImpl(
			JavaTypeDescriptor<J> jtd,
			JdbcTypeDescriptor std,
			String name) {
		super( jtd, std, name );
	}

	@Override
	protected MutabilityPlan<J> getMutabilityPlan() {
		//noinspection unchecked
		return ImmutableMutabilityPlan.INSTANCE;
	}
}
