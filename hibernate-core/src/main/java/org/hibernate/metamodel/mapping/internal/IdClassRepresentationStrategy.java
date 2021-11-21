/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.internal.EmbeddableInstantiatorPojoStandard;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.property.access.internal.PropertyAccessStrategyMixedImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * EmbeddableRepresentationStrategy for an IdClass mapping
 */
public class IdClassRepresentationStrategy implements EmbeddableRepresentationStrategy {
	private final JavaType<?> idClassType;
	private final EmbeddableInstantiator instantiator;

	public IdClassRepresentationStrategy(IdClassEmbeddable idClassEmbeddable) {
		this.idClassType = idClassEmbeddable.getMappedJavaTypeDescriptor();
		this.instantiator = new EmbeddableInstantiatorPojoStandard( idClassType, () -> idClassEmbeddable );
	}

	@Override
	public EmbeddableInstantiator getInstantiator() {
		return instantiator;
	}

	@Override
	public RepresentationMode getMode() {
		return RepresentationMode.POJO;
	}

	@Override
	public ReflectionOptimizer getReflectionOptimizer() {
		return null;
	}

	@Override
	public JavaType<?> getMappedJavaTypeDescriptor() {
		return idClassType;
	}

	@Override
	public PropertyAccess resolvePropertyAccess(Property bootAttributeDescriptor) {
		return PropertyAccessStrategyMixedImpl.INSTANCE.buildPropertyAccess(
				idClassType.getJavaTypeClass(),
				bootAttributeDescriptor.getName()
		);
	}
}
