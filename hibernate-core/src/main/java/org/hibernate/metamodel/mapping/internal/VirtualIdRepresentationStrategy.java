package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.mapping.Component;
import org.hibernate.accessor.MultiValueReader;
import org.hibernate.accessor.MultiValueWriter;
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
			@Nonnull VirtualIdEmbeddable virtualIdEmbeddable,
			@Nonnull EntityMappingType entityMappingType,
			@Nonnull Component bootDescriptor,
			@Nonnull RuntimeModelCreationContext creationContext) {
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
	@Nonnull
	public EmbeddableInstantiator getInstantiator() {
		return instantiator;
	}

	@Override
	@Nonnull
	public RepresentationMode getMode() {
		return RepresentationMode.POJO;
	}

	@Nullable
	@Override
	public MultiValueReader getMultiValueReader() {
		return null;
	}

	@Nullable
	@Override
	public MultiValueWriter getMultiValueWriter() {
		return null;
	}

	@Override
	@Nonnull
	public JavaType<?> getMappedJavaType() {
		return entityMappingType.getMappedJavaType();
	}

	@Override
	@Nullable
	public PropertyAccess resolvePropertyAccess(@Nonnull Property bootAttributeDescriptor) {
		return entityMappingType.getRepresentationStrategy().resolvePropertyAccess( bootAttributeDescriptor );
	}

	private static class InstantiatorAdapter implements StandardEmbeddableInstantiator {
		private final VirtualIdEmbeddable virtualIdEmbeddable;
		private final EntityInstantiator entityInstantiator;

		public InstantiatorAdapter(@Nonnull VirtualIdEmbeddable virtualIdEmbeddable, @Nonnull EntityMappingType entityMappingType) {
			this.virtualIdEmbeddable = virtualIdEmbeddable;
			entityInstantiator = entityMappingType.getRepresentationStrategy().getInstantiator();
		}

		@Override
		@Nonnull
		public Object instantiate(@Nonnull ValueAccess valuesAccess) {
			final Object instantiated = entityInstantiator.instantiate();
			if ( valuesAccess != null ) {
				final var values = valuesAccess.getValues();
				if ( values != null ) {
					virtualIdEmbeddable.setValues( instantiated, values );
				}
			}
			return instantiated;
		}

		@Override
		public boolean isInstance(@Nullable Object object) {
			return entityInstantiator.isInstance( object );
		}

		@Override
		public boolean isSameClass(@Nonnull Object object) {
			return entityInstantiator.isSameClass( object );
		}
	}
}
