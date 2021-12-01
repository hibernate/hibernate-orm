/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.Supplier;

import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.EntityInstantiator;
import org.hibernate.property.access.internal.PropertyAccessStrategyMixedImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class VirtualIdRepresentationStrategy implements EmbeddableRepresentationStrategy {
	private final EntityMappingType entityMappingType;
	private final EmbeddableInstantiator instantiator;

	public VirtualIdRepresentationStrategy(VirtualIdEmbeddable virtualIdEmbeddable, EntityMappingType entityMappingType) {
		this.entityMappingType = entityMappingType;
		this.instantiator = new InstantiatorAdapter( virtualIdEmbeddable, entityMappingType );
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
		return entityMappingType.getMappedJavaTypeDescriptor();
	}

	@Override
	public PropertyAccess resolvePropertyAccess(Property bootAttributeDescriptor) {
		return PropertyAccessStrategyMixedImpl.INSTANCE.buildPropertyAccess(
				entityMappingType.getMappedJavaTypeDescriptor().getJavaTypeClass(),
				bootAttributeDescriptor.getName()
		);
	}

	private static class InstantiatorAdapter implements EmbeddableInstantiator {
		private final VirtualIdEmbeddable virtualIdEmbeddable;
		private final EntityInstantiator entityInstantiator;

		public InstantiatorAdapter(VirtualIdEmbeddable virtualIdEmbeddable, EntityMappingType entityMappingType) {
			this.virtualIdEmbeddable = virtualIdEmbeddable;
			this.entityInstantiator = entityMappingType.getRepresentationStrategy().getInstantiator();
		}

		@Override
		public Object instantiate(Supplier<Object[]> valuesAccess, SessionFactoryImplementor sessionFactory) {
			final Object instantiated = entityInstantiator.instantiate( sessionFactory );
			if ( valuesAccess != null ) {
				final Object[] values = valuesAccess.get();
				if ( values != null ) {
					virtualIdEmbeddable.setValues( instantiated, values );
				}
			}
			return instantiated;
		}

		@Override
		public boolean isInstance(Object object, SessionFactoryImplementor sessionFactory) {
			return entityInstantiator.isInstance( object, sessionFactory );
		}

		@Override
		public boolean isSameClass(Object object, SessionFactoryImplementor sessionFactory) {
			return entityInstantiator.isSameClass( object, sessionFactory );
		}
	}
}
