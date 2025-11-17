/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.annotations.SoftDeleteType;

/**
 * Part of the boot model which can be soft-deleted
 *
 * @author Steve Ebersole
 */
public interface SoftDeletable {
	/**
	 * Enable soft-delete for this part of the model.
	 *
	 * @param indicatorColumn The column which indicates soft-deletion
	 * @param strategy The strategy for indicating soft-deletion
	 */
	void enableSoftDelete(Column indicatorColumn, SoftDeleteType strategy);

	/**
	 * The column which indicates soft-deletion.
	 */
	Column getSoftDeleteColumn();

	/**
	 * The strategy for indicating soft-deletion.
	 */
	SoftDeleteType getSoftDeleteStrategy();
}
