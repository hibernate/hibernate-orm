/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.relation;

import org.hibernate.envers.internal.entities.mapper.relation.component.MiddleComponentMapper;

/**
 * A data holder for a middle relation component (which is either the collection element or index):
 * - component mapper used to map the component to and from versions entities
 * - an index, which specifies in which element of the array returned by the query for reading the collection the data
 * of the component is
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public final class MiddleComponentData {
	private final MiddleComponentMapper componentMapper;
	private final int componentIndex;

	public MiddleComponentData(MiddleComponentMapper componentMapper) {
		this( componentMapper, 0 );
	}

	public MiddleComponentData(MiddleComponentMapper componentMapper, int componentIndex) {
		this.componentMapper = componentMapper;
		this.componentIndex = componentIndex;
	}

	public MiddleComponentMapper getComponentMapper() {
		return componentMapper;
	}

	public int getComponentIndex() {
		return componentIndex;
	}
}
