/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.Hibernate;
import org.hibernate.PersistentObjectException;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.SaveOrUpdateEvent;

/**
 * An event handler for save() events
 *
 * @author Gavin King
 *
 * @deprecated since {@link org.hibernate.Session#save} is deprecated
 */
@Deprecated(since="6")
public class DefaultSaveEventListener extends DefaultSaveOrUpdateEventListener {

	@Override
	protected Object performSaveOrUpdate(SaveOrUpdateEvent event) {
		// this implementation is supposed to tolerate incorrect unsaved-value
		// mappings, for the purpose of backward-compatibility
		final EntityEntry entry = event.getSession().getPersistenceContextInternal().getEntry( event.getEntity() );
		return entry != null && entry.getStatus() != Status.DELETED
				? entityIsPersistent( event )
				: entityIsTransient( event );
	}

	@Override
	protected boolean reassociateIfUninitializedProxy(Object object, SessionImplementor source) {
		if ( !Hibernate.isInitialized( object ) ) {
			throw new PersistentObjectException("uninitialized proxy passed to save()");
		}
		else {
			return false;
		}
	}
}
