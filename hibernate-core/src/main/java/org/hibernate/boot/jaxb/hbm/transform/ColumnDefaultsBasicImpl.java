/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.transform;

/**
 * @author Steve Ebersole
 */
public class ColumnDefaultsBasicImpl implements ColumnDefaults {
	/**
	 * Singleton access
	 */
	public static final ColumnDefaultsBasicImpl INSTANCE = new ColumnDefaultsBasicImpl();

	@Override
	public Boolean isNullable() {
		return Boolean.TRUE;
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

	@Override
	public Boolean isInsertable() {
		return Boolean.TRUE;
	}

	@Override
	public Boolean isUpdatable() {
		return Boolean.TRUE;
	}
}
