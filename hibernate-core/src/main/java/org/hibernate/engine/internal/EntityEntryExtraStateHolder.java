/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import org.hibernate.engine.spi.EntityEntryExtraState;

/**
 * Contains optional state from {@link org.hibernate.engine.spi.EntityEntry}.
 *
 * @author Emmanuel Bernard
 */
class EntityEntryExtraStateHolder implements EntityEntryExtraState {
	private EntityEntryExtraState next;
	private Object[] deletedState;

	Object[] getDeletedState() {
		return deletedState;
	}

	void setDeletedState(Object[] deletedState) {
		this.deletedState = deletedState;
	}

	//the following methods are handling extraState contracts.
	//they are not shared by a common superclass to avoid alignment padding
	//we are trading off duplication for padding efficiency
	@Override
	public void addExtraState(EntityEntryExtraState extraState) {
		if ( next == null ) {
			next = extraState;
		}
		else {
			next.addExtraState( extraState );
		}
	}

	@Override @SuppressWarnings("unchecked")
	public <T extends EntityEntryExtraState> T getExtraState(Class<T> extraStateType) {
		if ( next == null ) {
			return null;
		}
		if ( extraStateType.isAssignableFrom( next.getClass() ) ) {
			return (T) next;
		}
		else {
			return next.getExtraState( extraStateType );
		}
	}
}
