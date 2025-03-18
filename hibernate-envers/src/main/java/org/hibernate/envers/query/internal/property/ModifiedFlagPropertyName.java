/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query.internal.property;

import org.hibernate.envers.boot.internal.ModifiedColumnNameResolver;
import org.hibernate.envers.configuration.Configuration;

/**
 * PropertyNameGetter for modified flags
 *
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Chris Cranford
 */
public class ModifiedFlagPropertyName implements PropertyNameGetter {
	private final PropertyNameGetter propertyNameGetter;

	public ModifiedFlagPropertyName(PropertyNameGetter propertyNameGetter) {
		this.propertyNameGetter = propertyNameGetter;
	}

	@Override
	public String get(Configuration configuration) {
		final String suffix = configuration.getModifiedFlagsSuffix();
		return ModifiedColumnNameResolver.getName( propertyNameGetter.get( configuration ), suffix );
	}
}
