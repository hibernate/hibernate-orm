/*
 * SPDX-License-Identifier: Apache-2.0
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
