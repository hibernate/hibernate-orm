/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.spi;

import java.util.Comparator;

import org.hibernate.type.descriptor.java.MutabilityPlan;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractIdentifiableJavaDescriptor<J>
		extends AbstractManagedJavaDescriptor<J>
		implements IdentifiableJavaDescriptor<J> {

	public AbstractIdentifiableJavaDescriptor(
			String typeName,
			Class javaType,
			IdentifiableJavaDescriptor<? super J> superTypeDescriptor,
			MutabilityPlan mutabilityPlan,
			Comparator comparator) {
		super( typeName, javaType, superTypeDescriptor, mutabilityPlan, comparator );
	}

	@Override
	@SuppressWarnings("unchecked")
	public IdentifiableJavaDescriptor<? super J> getSuperType() {
		return (IdentifiableJavaDescriptor<? super J>) super.getSuperType();
	}
}
