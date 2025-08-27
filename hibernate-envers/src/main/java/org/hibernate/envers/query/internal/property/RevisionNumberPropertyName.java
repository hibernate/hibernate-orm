/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query.internal.property;

import org.hibernate.envers.configuration.Configuration;

/**
 * Used for specifying restrictions on the revision number, corresponding to an audit entity.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class RevisionNumberPropertyName implements PropertyNameGetter {
	@Override
	public String get(Configuration configuration) {
		return configuration.getRevisionNumberPath();
	}
}
