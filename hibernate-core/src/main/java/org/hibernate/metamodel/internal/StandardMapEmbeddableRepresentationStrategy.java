/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.Instantiator;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.property.access.internal.PropertyAccessStrategyMapImpl;
import org.hibernate.property.access.spi.PropertyAccess;

/**
 * @author Steve Ebersole
 */
public class StandardMapEmbeddableRepresentationStrategy implements EmbeddableRepresentationStrategy {
	private final DynamicMapInstantiator instantiator;

	public StandardMapEmbeddableRepresentationStrategy(
			Component bootDescriptor,
			RuntimeModelCreationContext creationContext) {
		this.instantiator = new DynamicMapInstantiator( bootDescriptor );
	}

	@Override
	public RepresentationMode getMode() {
		return RepresentationMode.MAP;
	}

	@Override
	public ReflectionOptimizer getReflectionOptimizer() {
		return null;
	}

	@Override
	public PropertyAccess resolvePropertyAccess(Property bootAttributeDescriptor) {
		return PropertyAccessStrategyMapImpl.INSTANCE.buildPropertyAccess(
				null,
				bootAttributeDescriptor.getName()
		);
	}

	@Override
	public <J> Instantiator<J> getInstantiator() {
		//noinspection unchecked
		return (Instantiator) instantiator;
	}
}
