/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
