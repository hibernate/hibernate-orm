/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query.internal.property;

import org.hibernate.envers.configuration.Configuration;

/**
 * Provides a function to get the name of a property, which is used in a query, to apply some restrictions on it.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public interface PropertyNameGetter {
	/**
	 * @param configuration the envers configuration
	 * @return Name of the property, to be used in a query.
	 */
	String get(Configuration configuration);
}
