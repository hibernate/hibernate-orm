/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.util.function.Supplier;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.accessor.HibernateAccessorInstantiator;

/**
 * Support for instantiating embeddables as POJO representation
 * using bytecode optimizer
 */
public class EmbeddableInstantiatorPojoOptimized
		extends AbstractPojoInstantiator
		implements StandardEmbeddableInstantiator {
	private final Supplier<EmbeddableMappingType> embeddableMappingAccess;
	private final HibernateAccessorInstantiator<?> instantiator;

	public EmbeddableInstantiatorPojoOptimized(
			Class<?> embeddableClass,
			Supplier<EmbeddableMappingType> embeddableMappingAccess,
			HibernateAccessorInstantiator<?> instantiator) {
		super( embeddableClass );
		this.embeddableMappingAccess = embeddableMappingAccess;
		this.instantiator = instantiator;
	}

	@Override
	public Object instantiate(ValueAccess valuesAccess) {
		final Object embeddable = instantiator.create();
		final var embeddableMapping = embeddableMappingAccess.get();
		final var values = valuesAccess.getValues();
		if ( values != null ) {
			embeddableMapping.setValues( embeddable, values );
		}
		return embeddable;
	}
}
