/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.transform;

import org.hibernate.mapping.Property;

/**
 * @author Steve Ebersole
 */
public class ColumnDefaultsProperty implements ColumnDefaults {
	private final Property property;

	public ColumnDefaultsProperty(Property property) {
		this.property = property;
	}

	@Override
	public Boolean isNullable() {
		return property.isOptional();
	}

	@Override
	public Boolean isInsertable() {
		return property.isInsertable();
	}

	@Override
	public Boolean isUpdatable() {
		return property.isUpdatable();
	}

	@Override
	public Integer getLength() {
		return null;
	}

	@Override
	public Integer getScale() {
		return null;
	}

	@Override
	public Integer getPrecision() {
		return null;
	}

	@Override
	public Boolean isUnique() {
		return Boolean.FALSE;
	}
}
