//$Id: $
package org.hibernate.event;

import java.io.Serializable;

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
