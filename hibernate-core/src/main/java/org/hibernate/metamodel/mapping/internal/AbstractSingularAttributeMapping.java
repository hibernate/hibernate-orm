/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.generator.Generator;
import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.results.graph.FetchOptions;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSingularAttributeMapping
		extends AbstractStateArrayContributorMapping
		implements SingularAttributeMapping {

	public AbstractSingularAttributeMapping(
			String name,
			int stateArrayPosition,
			int fetchableIndex,
			AttributeMetadata attributeMetadata,
			FetchOptions mappedFetchOptions,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess) {
		super( name, attributeMetadata, mappedFetchOptions, stateArrayPosition, fetchableIndex, declaringType, propertyAccess );
	}

	public AbstractSingularAttributeMapping(
			String name,
			int stateArrayPosition,
			int fetchableIndex,
			AttributeMetadata attributeMetadata,
			FetchTiming fetchTiming,
			FetchStyle fetchStyle,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess) {
		super( name, attributeMetadata, fetchTiming, fetchStyle, stateArrayPosition, fetchableIndex, declaringType, propertyAccess );
	}

	/**
	 * For Hibernate Reactive
	 */
	protected AbstractSingularAttributeMapping( AbstractSingularAttributeMapping original ) {
		super( original );
	}

	@Override
	public Generator getGenerator() {
		return findContainingEntityMapping().getEntityPersister().getGenerators()[getStateArrayPosition()];
	}

}
