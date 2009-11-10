/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
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
package org.hibernate.impl;

/**
 * An implementation of NonFlushedChanges.
 *
 * @author Gail Badner
 */

import org.hibernate.engine.NonFlushedChanges;
import org.hibernate.engine.ActionQueue;
import org.hibernate.engine.StatefulPersistenceContext;
import org.hibernate.event.EventSource;
import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;

import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NonFlushedChangesImpl implements NonFlushedChanges {

	private static final Logger log = LoggerFactory.getLogger(NonFlushedChangesImpl.class);

	private static class SessionNonFlushedChanges implements Serializable {
		private transient EntityMode entityMode;
		private transient ActionQueue actionQueue;
		private transient StatefulPersistenceContext persistenceContext;

		public SessionNonFlushedChanges(EventSource session) {
			this.entityMode = session.getEntityMode();
			this.actionQueue = session.getActionQueue();
			this.persistenceContext = ( StatefulPersistenceContext ) session.getPersistenceContext();
		}

		private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
			ois.defaultReadObject();
			entityMode = EntityMode.parse( ( String ) ois.readObject() );
			persistenceContext = StatefulPersistenceContext.deserialize( ois, null );
			actionQueue = ActionQueue.deserialize( ois, null );
		}

		private void writeObject(ObjectOutputStream oos) throws IOException {
			log.trace( "serializing SessionNonFlushedChanges" );
			oos.defaultWriteObject();
			oos.writeObject( entityMode.toString() );
			persistenceContext.serialize( oos );
			actionQueue.serialize( oos );
		}
	}
	private Map nonFlushedChangesByEntityMode = new HashMap();

	public NonFlushedChangesImpl( EventSource session ) {
		extractFromSession( session );
	}

	public void extractFromSession(EventSource session) {
		if ( nonFlushedChangesByEntityMode.containsKey( session.getEntityMode() ) ) {
			throw new AssertionFailure( "Already has non-flushed changes for entity mode: " + session.getEntityMode() );
		}
		nonFlushedChangesByEntityMode.put( session.getEntityMode(), new SessionNonFlushedChanges( session ) );
	}

	private SessionNonFlushedChanges getSessionNonFlushedChanges(EntityMode entityMode) {
		return ( SessionNonFlushedChanges ) nonFlushedChangesByEntityMode.get( entityMode );
	}

	/* package-protected */
	ActionQueue getActionQueue(EntityMode entityMode) {
		return getSessionNonFlushedChanges( entityMode ).actionQueue;
	}	

	/* package-protected */
	StatefulPersistenceContext getPersistenceContext(EntityMode entityMode) {
		return getSessionNonFlushedChanges( entityMode ).persistenceContext;
	}

	public void clear() {
		nonFlushedChangesByEntityMode.clear();
	}
}