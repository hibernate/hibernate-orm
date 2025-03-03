/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

/**
 * Model part which can be soft-deleted
 *
 * @author Steve Ebersole
 */
public interface SoftDeletableModelPart extends ModelPartContainer {
	/**
	 * Get the mapping of the soft-delete indicator
	 */
	SoftDeleteMapping getSoftDeleteMapping();

	/**
	 * Details about the table which holds the soft-delete column.
	 */
	TableDetails getSoftDeleteTableDetails();
}
