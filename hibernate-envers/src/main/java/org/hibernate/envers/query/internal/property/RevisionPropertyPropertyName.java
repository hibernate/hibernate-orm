/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query.internal.property;

import org.hibernate.envers.configuration.Configuration;

/**
 * Used for specifying restrictions on a property of the revision entity, which is associated with an audit entity.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class RevisionPropertyPropertyName implements PropertyNameGetter {
	private final String propertyName;

	public RevisionPropertyPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	@Override
	public String get(Configuration configuration) {
		return configuration.getRevisionPropertyPath( propertyName );
	}
}
