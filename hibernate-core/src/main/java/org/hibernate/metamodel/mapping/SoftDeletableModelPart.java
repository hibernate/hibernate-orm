/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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

	TableDetails getSoftDeleteTableDetails();
}
