/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.property.access.internal.PropertyAccessStrategyMapImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class EmbeddableRepresentationStrategyMap implements EmbeddableRepresentationStrategy {
	private final JavaType<Map<String,?>> mapJavaType;
	private final EmbeddableInstantiator instantiator;

	public EmbeddableRepresentationStrategyMap(
			Component bootDescriptor,
			Supplier<EmbeddableMappingType> runtimeDescriptorAccess,
			EmbeddableInstantiator customInstantiator,
			RuntimeModelCreationContext creationContext) {
		mapJavaType =
				creationContext.getTypeConfiguration().getJavaTypeRegistry()
						.getDescriptor( Map.class );
		instantiator =
				customInstantiator == null
						? new EmbeddableInstantiatorDynamicMap( bootDescriptor, runtimeDescriptorAccess )
						: customInstantiator;
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
	public JavaType<?> getMappedJavaType() {
		return mapJavaType;
	}

	@Override
	public PropertyAccess resolvePropertyAccess(Property bootAttributeDescriptor) {
		return PropertyAccessStrategyMapImpl.INSTANCE.buildPropertyAccess(
				null,
				bootAttributeDescriptor.getName(),
				true );
	}

	@Override
	public EmbeddableInstantiator getInstantiator() {
		return instantiator;
	}
}
