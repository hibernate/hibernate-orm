/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

/**
 * @author Steve Ebersole
 */
public interface SoftDeletable {
	void enableSoftDelete(Column indicatorColumn);
	Column getSoftDeleteColumn();
}
