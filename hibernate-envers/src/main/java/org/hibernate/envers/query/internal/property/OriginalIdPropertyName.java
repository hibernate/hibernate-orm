/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query.internal.property;

import org.hibernate.envers.configuration.Configuration;

/**
 * Used for specifying restrictions on the identifier.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class OriginalIdPropertyName implements PropertyNameGetter {
	private final String idPropertyName;

	public OriginalIdPropertyName(String idPropertyName) {
		this.idPropertyName = idPropertyName;
	}

	@Override
	public String get(Configuration configuration) {
		return configuration.getOriginalIdPropertyName() + "." + idPropertyName;
	}
}
