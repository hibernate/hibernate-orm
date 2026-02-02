/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.InstantiationException;
import org.hibernate.persister.state.StateManagement;
import org.hibernate.persister.state.internal.StandardStateManagement;

/**
 * Abstracts over things which can have a {@linkplain StateManagement
 * customized state management strategy}, providing slots to plug in
 * extra columns related to custom state management.
 *
 * @author Gavin King
 *
 * @see org.hibernate.annotations.Temporal
 * @see org.hibernate.annotations.Audited
 * @see org.hibernate.annotations.SoftDelete
 */
public interface Stateful {

	void setStateManagementType(Class<? extends StateManagement> stateManagementType);

	Class<? extends StateManagement> getStateManagementType();

	Table getAuxiliaryTable();

	void setAuxiliaryTable(Table auxiliaryTable);

	Table getMainTable();

	boolean isMainTablePartitioned();

	void setMainTablePartitioned(boolean partitioned);

	Column getAuxiliaryColumn(String column);

	void addAuxiliaryColumn(String name, Column column);

	boolean isAuxiliaryColumnInPrimaryKey();

	void setAuxiliaryColumnInPrimaryKey(String key);

	boolean isPrimaryKeyDisabled();

	void setPrimaryKeyDisabled(boolean disabled);

	default StateManagement getStateManagement() {
		final var stateManagementType = getStateManagementType();
		if ( stateManagementType == null ) {
			return StandardStateManagement.INSTANCE;
		}
		else {
			try {
				return (StateManagement)
						stateManagementType
								.getDeclaredField( "INSTANCE" )
								.get( null );
			}
			catch (IllegalAccessException | NoSuchFieldException e) {
				throw new InstantiationException( "Could not create StateManagement",
						stateManagementType, e );
			}
		}
	}
}
