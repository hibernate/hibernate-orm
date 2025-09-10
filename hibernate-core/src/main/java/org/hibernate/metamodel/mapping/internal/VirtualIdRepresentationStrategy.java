/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.internal.EmbeddableInstantiatorProxied;
import org.hibernate.metamodel.internal.StandardEmbeddableInstantiator;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.EntityInstantiator;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.internal.util.ReflectHelper.isAbstractClass;

/**
 * @author Steve Ebersole
 */
public class VirtualIdRepresentationStrategy implements EmbeddableRepresentationStrategy {
	private final EntityMappingType entityMappingType;
	private final EmbeddableInstantiator instantiator;

	public VirtualIdRepresentationStrategy(
			VirtualIdEmbeddable virtualIdEmbeddable,
			EntityMappingType entityMappingType,
			Component bootDescriptor,
			RuntimeModelCreationContext creationContext) {
		this.entityMappingType = entityMappingType;
		if ( bootDescriptor.getComponentClassName() != null
				&& isAbstractClass( bootDescriptor.getComponentClass() ) ) {
			instantiator = new EmbeddableInstantiatorProxied(
					bootDescriptor.getComponentClass(),
					() -> virtualIdEmbeddable,
					creationContext.getServiceRegistry().requireService( ProxyFactoryFactory.class )
							.buildBasicProxyFactory( bootDescriptor.getComponentClass() )

			);
		}
		else {
			instantiator = new InstantiatorAdapter( virtualIdEmbeddable, entityMappingType );
		}
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
	public JavaType<?> getMappedJavaType() {
		return entityMappingType.getMappedJavaType();
	}

	@Override
	public PropertyAccess resolvePropertyAccess(Property bootAttributeDescriptor) {
		return entityMappingType.getRepresentationStrategy().resolvePropertyAccess( bootAttributeDescriptor );
	}

	private static class InstantiatorAdapter implements StandardEmbeddableInstantiator {
		private final VirtualIdEmbeddable virtualIdEmbeddable;
		private final EntityInstantiator entityInstantiator;

		public InstantiatorAdapter(VirtualIdEmbeddable virtualIdEmbeddable, EntityMappingType entityMappingType) {
			this.virtualIdEmbeddable = virtualIdEmbeddable;
			entityInstantiator = entityMappingType.getRepresentationStrategy().getInstantiator();
		}

		@Override
		public Object instantiate(ValueAccess valuesAccess) {
			final Object instantiated = entityInstantiator.instantiate();
			if ( valuesAccess != null ) {
				final Object[] values = valuesAccess.getValues();
				if ( values != null ) {
					virtualIdEmbeddable.setValues( instantiated, values );
				}
			}
			return instantiated;
		}

		@Override
		public boolean isInstance(Object object) {
			return entityInstantiator.isInstance( object );
		}

		@Override
		public boolean isSameClass(Object object) {
			return entityInstantiator.isSameClass( object );
		}
	}
}
