/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.util.function.Supplier;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.ValueAccess;

import static org.hibernate.bytecode.spi.ReflectionOptimizer.InstantiationOptimizer;

/**
 * Support for instantiating embeddables as POJO representation
 * using bytecode optimizer
 */
public class EmbeddableInstantiatorPojoOptimized extends AbstractPojoInstantiator implements StandardEmbeddableInstantiator {
	private final Supplier<EmbeddableMappingType> embeddableMappingAccess;
	private final InstantiationOptimizer instantiationOptimizer;

	public EmbeddableInstantiatorPojoOptimized(
			Class<?> embeddableClass,
			Supplier<EmbeddableMappingType> embeddableMappingAccess,
			InstantiationOptimizer instantiationOptimizer) {
		super( embeddableClass );
		this.embeddableMappingAccess = embeddableMappingAccess;
		this.instantiationOptimizer = instantiationOptimizer;
	}

	@Override
	public Object instantiate(ValueAccess valuesAccess) {
		final Object embeddable = instantiationOptimizer.newInstance();
		final EmbeddableMappingType embeddableMapping = embeddableMappingAccess.get();
		final Object[] values = valuesAccess.getValues();
		if ( values != null ) {
			embeddableMapping.setValues( embeddable, values );
		}
		return embeddable;
	}
}
