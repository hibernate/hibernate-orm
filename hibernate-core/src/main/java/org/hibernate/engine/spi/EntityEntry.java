/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2014, Red Hat Inc. or third-party contributors as
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

package org.hibernate.engine.spi;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.hibernate.LockMode;
import org.hibernate.persister.entity.EntityPersister;

/**
 * We need an entry to tell us all about the current state of an object with respect to its persistent state.
 *
 * @author Gavin King
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface EntityEntry extends EntityEntryExtraState {

	LockMode getLockMode();

	void setLockMode(LockMode lockMode);

	Status getStatus();

	void setStatus(Status status);

	Serializable getId();

	Object[] getLoadedState();

	Object[] getDeletedState();

	void setDeletedState(Object[] deletedState);

	boolean isExistsInDatabase();

	Object getVersion();

	EntityPersister getPersister();

	EntityKey getEntityKey();

	String getEntityName();

	boolean isBeingReplicated();

	Object getRowId();

	void postUpdate(Object entity, Object[] updatedState, Object nextVersion);

	void postDelete();

	void postInsert(Object[] insertedState);

	boolean isNullifiable(boolean earlyInsert, SessionImplementor session);

	Object getLoadedValue(String propertyName);

	boolean requiresDirtyCheck(Object entity);

	boolean isModifiableEntity();

	void forceLocked(Object entity, Object nextVersion);

	boolean isReadOnly();

	void setReadOnly(boolean readOnly, Object entity);

	boolean isLoadedWithLazyPropertiesUnfetched();

	public void serialize(ObjectOutputStream oos) throws IOException;

}
