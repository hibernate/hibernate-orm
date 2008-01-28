/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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

//$Id: $
package org.hibernate.event;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;

/**
 * Defines a base class for events involving collections.
 *
 * @author Gail Badner
 */
public abstract class AbstractCollectionEvent extends AbstractEvent {

	private final PersistentCollection collection;
	private final Object affectedOwner;

	public AbstractCollectionEvent(PersistentCollection collection, EventSource source, Object affectedOwner) {
		super(source);
		this.collection = collection;
		this.affectedOwner = affectedOwner;
	}

	protected static Object getLoadedOwner( PersistentCollection collection, EventSource source ) {
		return ( ( SessionImplementor ) source ).getPersistenceContext().getLoadedCollectionOwner( collection );
	}

	public PersistentCollection getCollection() {
		return collection;
	}

	public Object getAffectedOwner() {
		return affectedOwner;
	}
}
