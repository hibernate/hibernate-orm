/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.util.Map;
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
			Component bootDescriptor,
			Supplier<EmbeddableMappingType> runtimeDescriptorAccess) {
		super( bootDescriptor.getRoleName() );
		this.runtimeDescriptorAccess = runtimeDescriptorAccess;
	}

	@Override
	public Object instantiate(ValueAccess valuesAccess) {
		final Map<?,?> dataMap = generateDataMap();
		final Object[] values = valuesAccess == null ? null : valuesAccess.getValues();
		if ( values != null ) {
			final EmbeddableMappingType mappingType = runtimeDescriptorAccess.get();
			mappingType.setValues( dataMap, values );
		}
		return dataMap;
	}
}
