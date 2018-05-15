/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.spi;

import java.util.Comparator;

import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;

/**
 * Base support for all ManagedType implementations.managed types, which is the JPA term for commonality between entity, embeddable and
 * "mapped superclass" types.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractManagedJavaDescriptor<J>
		extends AbstractJavaDescriptor<J>
		implements ManagedJavaDescriptor<J>, TypeConfigurationAware {

	private final ManagedJavaDescriptor<? super J> superTypeDescriptor;

	private TypeConfiguration typeConfiguration;

	public AbstractManagedJavaDescriptor(
			String typeName,
			Class javaType,
			ManagedJavaDescriptor<? super J> superTypeDescriptor,
			MutabilityPlan mutabilityPlan,
			Comparator comparator) {
		super( typeName, javaType, mutabilityPlan, comparator );
		this.superTypeDescriptor = superTypeDescriptor;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	public void setTypeConfiguration(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
	}

	@Override
	public ManagedJavaDescriptor<? super J> getSuperType() {
		return superTypeDescriptor;
	}
}
