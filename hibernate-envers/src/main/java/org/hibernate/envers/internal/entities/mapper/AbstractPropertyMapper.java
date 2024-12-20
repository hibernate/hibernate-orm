/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper;

/**
 * Abstract implementation of a {@link PropertyMapper}.
 *
 * @author Chris Cranford
 */
public abstract class AbstractPropertyMapper extends AbstractMapper implements PropertyMapper {
	private boolean map;

	@Override
	public void markAsDynamicComponentMap() {
		this.map = true;
	}

	@Override
	public boolean isDynamicComponentMap() {
		return map;
	}
}
