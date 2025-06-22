/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.id;

import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.mapping.Component;

/**
 * An identifier mapper that is meant to support nested {@link jakarta.persistence.Embeddable} instances
 * inside an existing {@link jakarta.persistence.EmbeddedId} identifier hierarchy.
 *
 * @author Chris Cranford
 */
public class NestedEmbeddedIdMapper extends EmbeddedIdMapper {
	public NestedEmbeddedIdMapper(PropertyData propertyData, Component component) {
		super( propertyData, component );
	}
}
