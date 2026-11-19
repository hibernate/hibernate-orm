package org.hibernate.metamodel.internal;

import java.util.Map;
import java.util.function.Supplier;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.accessor.MultiValueReader;
import org.hibernate.accessor.MultiValueWriter;
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
			@Nonnull Component bootDescriptor,
			@Nonnull Supplier<EmbeddableMappingType> runtimeDescriptorAccess,
			@Nullable EmbeddableInstantiator customInstantiator,
			@Nonnull RuntimeModelCreationContext creationContext) {
		mapJavaType =
				creationContext.getTypeConfiguration().getJavaTypeRegistry()
						.getDescriptor( Map.class );
		instantiator =
				customInstantiator == null
						? new EmbeddableInstantiatorDynamicMap( bootDescriptor, runtimeDescriptorAccess )
						: customInstantiator;
	}

	@Override
	@Nonnull
	public RepresentationMode getMode() {
		return RepresentationMode.MAP;
	}

	@Override
	@Nullable
	public MultiValueReader getMultiValueReader() {
		return null;
	}

	@Override
	@Nullable
	public MultiValueWriter getMultiValueWriter() {
		return null;
	}

	@Override
	@Nonnull
	public JavaType<?> getMappedJavaType() {
		return mapJavaType;
	}

	@Override
	@Nonnull
	public PropertyAccess resolvePropertyAccess(@Nonnull Property bootAttributeDescriptor) {
		return PropertyAccessStrategyMapImpl.INSTANCE.buildPropertyAccess(
				null, // because Maps don't get a service
				null,
				bootAttributeDescriptor.getName(), true );
	}

	@Override
	@Nonnull
	public EmbeddableInstantiator getInstantiator() {
		return instantiator;
	}
}
