/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.transform;

/**
 * @author Steve Ebersole
 */
interface ColumnDefaults {
	Boolean isNullable();

	Integer getLength();

	Integer getScale();

	Integer getPrecision();

	Boolean isUnique();

	Boolean isInsertable();

	Boolean isUpdateable();
}
