package org.hibernate.metamodel.internal;

import jakarta.annotation.Nonnull;

import java.util.function.Supplier;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.accessor.Instantiator;

/**
 * Support for instantiating embeddables as POJO representation
 * using bytecode optimizer
 */
public class EmbeddableInstantiatorPojoOptimized
		extends AbstractPojoInstantiator
		implements StandardEmbeddableInstantiator {
	private final Supplier<EmbeddableMappingType> embeddableMappingAccess;
	private final Instantiator<?> instantiator;

	public EmbeddableInstantiatorPojoOptimized(
			@Nonnull Class<?> embeddableClass,
			@Nonnull Supplier<EmbeddableMappingType> embeddableMappingAccess,
			@Nonnull Instantiator<?> instantiator) {
		super( embeddableClass );
		this.embeddableMappingAccess = embeddableMappingAccess;
		this.instantiator = instantiator;
	}

	@Override
	@Nonnull
	public Object instantiate(@Nonnull ValueAccess valuesAccess) {
		final Object embeddable = instantiator.create();
		final var embeddableMapping = embeddableMappingAccess.get();
		final var values = valuesAccess.getValues();
		if ( values != null ) {
			embeddableMapping.setValues( embeddable, values );
		}
		return embeddable;
	}
}
