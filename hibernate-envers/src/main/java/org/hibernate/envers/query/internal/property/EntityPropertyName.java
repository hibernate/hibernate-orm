/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query.internal.property;

import org.hibernate.envers.configuration.Configuration;

/**
 * Used for specifying restrictions on a property of an audited entity.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class EntityPropertyName implements PropertyNameGetter {
	private final String propertyName;

	public EntityPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public String get(Configuration configuration) {
		return propertyName;
	}
}
