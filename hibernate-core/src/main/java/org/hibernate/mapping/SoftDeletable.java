/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.Incubating;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.persister.state.internal.SoftDeleteStateManagement;

/**
 * Part of the boot model which can be soft-deleted
 *
 * @author Steve Ebersole
 *
 * @deprecated This is no longer needed after the
 *             introduction of {@link Stateful}.
 */
@Incubating @Deprecated(forRemoval = true)
public interface SoftDeletable extends Stateful {

	String INDICATOR = "indicator";

	/**
	 * Enable soft-delete for this part of the model.
	 *
	 * @param indicatorColumn The column which indicates soft-deletion
	 * @param strategy The strategy for indicating soft-deletion
	 */
	default void enableSoftDelete(Column indicatorColumn, SoftDeleteType strategy) {
		addAuxiliaryColumn( INDICATOR, indicatorColumn );
		setStateManagementType( SoftDeleteStateManagement.class );
	}

	/**
	 * The column which indicates soft-deletion.
	 */
	default Column getSoftDeleteColumn() {
		return getAuxiliaryColumn( INDICATOR );
	}

	/**
	 * The strategy for indicating soft-deletion.
	 */
	SoftDeleteType getSoftDeleteStrategy();
}
