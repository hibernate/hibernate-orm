/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import java.util.Comparator;

import org.hibernate.type.descriptor.java.spi.IdentifiableJavaDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.spi.IdentifiableType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractIdentifiableType extends AbstractManagedType implements IdentifiableType {
	public AbstractIdentifiableType(IdentifiableType superType, IdentifiableJavaDescriptor javaTypeDescriptor) {
		super( superType, javaTypeDescriptor );
	}

	public AbstractIdentifiableType(
			IdentifiableType superType,
			IdentifiableJavaDescriptor javaTypeDescriptor,
			MutabilityPlan mutabilityPlan,
			Comparator comparator) {
		super( superType, javaTypeDescriptor, mutabilityPlan, comparator );
	}

	@Override
	public IdentifiableJavaDescriptor getJavaTypeDescriptor() {
		return (IdentifiableJavaDescriptor) super.getJavaTypeDescriptor();
	}

	@Override
	public IdentifiableType getSuperType() {
		return (IdentifiableType) super.getSuperType();
	}
}
