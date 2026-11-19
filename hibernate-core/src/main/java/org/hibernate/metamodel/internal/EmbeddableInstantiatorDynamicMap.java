package org.hibernate.metamodel.internal;

import jakarta.annotation.Nonnull;

import java.util.function.Supplier;

import org.hibernate.mapping.Component;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.ValueAccess;

/**
 * Support for instantiating embeddables as dynamic-map representation
 *
 * @author Steve Ebersole
 */
public class EmbeddableInstantiatorDynamicMap
		extends AbstractDynamicMapInstantiator
		implements StandardEmbeddableInstantiator {
	private final Supplier<EmbeddableMappingType> runtimeDescriptorAccess;

	public EmbeddableInstantiatorDynamicMap(
			@Nonnull Component bootDescriptor,
			@Nonnull Supplier<EmbeddableMappingType> runtimeDescriptorAccess) {
		super( bootDescriptor.getRoleName() );
		this.runtimeDescriptorAccess = runtimeDescriptorAccess;
	}

	@Override
	@Nonnull
	public Object instantiate(@Nonnull ValueAccess valuesAccess) {
		final var dataMap = generateDataMap();
		final var values = valuesAccess == null ? null : valuesAccess.getValues();
		if ( values != null ) {
			final var mappingType = runtimeDescriptorAccess.get();
			mappingType.setValues( dataMap, values );
		}
		return dataMap;
	}
}
