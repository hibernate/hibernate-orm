/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import org.hibernate.engine.spi.EntityEntryExtraState;

/**
 * Contains optional state from {@link org.hibernate.engine.spi.EntityEntry}.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class EntityEntryExtraStateHolder implements EntityEntryExtraState {
	private EntityEntryExtraState next;
	private Object[] deletedState;

	public Object[] getDeletedState() {
		return deletedState;
	}

	public void setDeletedState(Object[] deletedState) {
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

	@Override
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
