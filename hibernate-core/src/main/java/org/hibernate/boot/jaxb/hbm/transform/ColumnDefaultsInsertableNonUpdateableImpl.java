/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.transform;

/**
 * @author Steve Ebersole
 */
class ColumnDefaultsInsertableNonUpdateableImpl implements ColumnDefaults {
	/**
	 * Singleton access
	 */
	public static final ColumnDefaultsInsertableNonUpdateableImpl INSTANCE = new ColumnDefaultsInsertableNonUpdateableImpl();

	@Override
	public Boolean isNullable() {
		return null;
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
		return null;
	}

	@Override
	public Boolean isInsertable() {
		return true;
	}

	@Override
	public Boolean isUpdatable() {
		return false;
	}
}
