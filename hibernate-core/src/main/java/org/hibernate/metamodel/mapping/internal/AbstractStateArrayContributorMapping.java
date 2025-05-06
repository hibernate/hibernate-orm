/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.results.graph.FetchOptions;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractStateArrayContributorMapping
		extends AbstractAttributeMapping
		implements FetchOptions {

	private final FetchTiming fetchTiming;
	private final FetchStyle fetchStyle;

	public AbstractStateArrayContributorMapping(
			String name,
			AttributeMetadata attributeMetadata,
			FetchTiming fetchTiming,
			FetchStyle fetchStyle,
			int stateArrayPosition,
			int fetchableIndex,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess) {
		super( name, fetchableIndex, declaringType, attributeMetadata, stateArrayPosition, propertyAccess );
		this.fetchTiming = fetchTiming;
		this.fetchStyle = fetchStyle;
	}

	public AbstractStateArrayContributorMapping(
			String name,
			AttributeMetadata attributeMetadata,
			FetchOptions mappedFetchOptions,
			int stateArrayPosition,
			int fetchableIndex,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess) {
		this(
				name,
				attributeMetadata,
				mappedFetchOptions.getTiming(),
				mappedFetchOptions.getStyle(),
				stateArrayPosition,
				fetchableIndex,
				declaringType,
				propertyAccess
		);
	}

	/**
	 * For Hibernate Reactive
	 */
	protected AbstractStateArrayContributorMapping(AbstractStateArrayContributorMapping original) {
		super( original );
		this.fetchTiming = original.fetchTiming;
		this.fetchStyle = original.fetchStyle;
	}

	@Override
	public String getFetchableName() {
		return getAttributeName();
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return this;
	}

	@Override
	public FetchStyle getStyle() {
		return fetchStyle;
	}

	@Override
	public FetchTiming getTiming() {
		return fetchTiming;
	}
}
