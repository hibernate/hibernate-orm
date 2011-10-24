/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.internal;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.jboss.logging.Logger;

import org.hibernate.engine.internal.StatefulPersistenceContext;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.NonFlushedChanges;
import org.hibernate.event.spi.EventSource;

public final class NonFlushedChangesImpl implements NonFlushedChanges, Serializable {
    private static final Logger LOG = Logger.getLogger( NonFlushedChangesImpl.class.getName() );

	private transient ActionQueue actionQueue;
	private transient StatefulPersistenceContext persistenceContext;

	public NonFlushedChangesImpl(EventSource session) {
		this.actionQueue = session.getActionQueue();
		this.persistenceContext = ( StatefulPersistenceContext ) session.getPersistenceContext();
	}

	/* package-protected */
	ActionQueue getActionQueue() {
		return actionQueue;
	}

	/* package-protected */
	StatefulPersistenceContext getPersistenceContext() {
		return persistenceContext;
	}

	public void clear() {
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		LOG.trace( "Deserializing NonFlushedChangesImpl" );
		ois.defaultReadObject();
		persistenceContext = StatefulPersistenceContext.deserialize( ois, null );
		actionQueue = ActionQueue.deserialize( ois, null );
	}

	private void writeObject(ObjectOutputStream oos) throws IOException {
		LOG.trace( "Serializing NonFlushedChangesImpl" );
		oos.defaultWriteObject();
		persistenceContext.serialize( oos );
		actionQueue.serialize( oos );
	}

}