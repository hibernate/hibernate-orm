/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.transform;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmKeyType;

/**
 * ColumnDefaults implementation for collection key columns that respects the
 * HBM {@code not-null} attribute.
 * <p>
 * In HBM XML, {@code <key not-null="true"/>} should map to
 * {@code <join-column nullable="false"/>} in ORM XML.
 *
 * @author Andrea Boriero
 */
class ColumnDefaultsCollectionKeyImpl implements ColumnDefaults {
	private final JaxbHbmKeyType key;

	public ColumnDefaultsCollectionKeyImpl(JaxbHbmKeyType key) {
		this.key = key;
	}

	@Override
	public Boolean isNullable() {
		// Respect the not-null attribute from the HBM key element
		// HBM: not-null="true" means ORM XML: nullable="false"
		return key.isNotNull() != null ? !key.isNotNull() : Boolean.TRUE;
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
