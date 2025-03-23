/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.metamodel.mapping.DiscriminatorValueDetails;
import org.hibernate.metamodel.mapping.EntityMappingType;

/**
 * @author Steve Ebersole
 */
public class DiscriminatorValueDetailsImpl implements DiscriminatorValueDetails {
	private final Object value;
	private final EntityMappingType matchedEntityDescriptor;

	public DiscriminatorValueDetailsImpl(Object value, EntityMappingType matchedEntityDescriptor) {
		this.value = value;
		this.matchedEntityDescriptor = matchedEntityDescriptor;
	}

	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public EntityMappingType getIndicatedEntity() {
		return matchedEntityDescriptor;
	}
}
