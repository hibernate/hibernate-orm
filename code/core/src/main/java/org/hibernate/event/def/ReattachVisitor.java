//$Id: ReattachVisitor.java 10948 2006-12-07 21:53:10Z steve.ebersole@jboss.com $
package org.hibernate.event.def;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.action.CollectionRemoveAction;
import org.hibernate.event.EventSource;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.type.Type;

/**
 * Abstract superclass of visitors that reattach collections.
 *
 * @author Gavin King
 */
public abstract class ReattachVisitor extends ProxyVisitor {

	private static final Log log = LogFactory.getLog( ReattachVisitor.class );

	private final Serializable ownerIdentifier;
	private final Object owner;

	public ReattachVisitor(EventSource session, Serializable ownerIdentifier, Object owner) {
		super( session );
		this.ownerIdentifier = ownerIdentifier;
		this.owner = owner;
	}

	/**
	 * Retrieve the identifier of the entity being visited.
	 *
	 * @return The entity's identifier.
	 */
	final Serializable getOwnerIdentifier() {
		return ownerIdentifier;
	}

	/**
	 * Retrieve the entity being visited.
	 *
	 * @return The entity.
	 */
	final Object getOwner() {
		return owner;
	}

	/**
	 * {@inheritDoc}
	 */
	Object processComponent(Object component, AbstractComponentType componentType) throws HibernateException {
		Type[] types = componentType.getSubtypes();
		if ( component == null ) {
			processValues( new Object[types.length], types );
		}
		else {
			super.processComponent( component, componentType );
		}

		return null;
	}

	/**
	 * Schedules a collection for deletion.
	 *
	 * @param role The persister representing the collection to be removed.
	 * @param collectionKey The collection key (differs from owner-id in the case of property-refs).
	 * @param source The session from which the request originated.
	 * @throws HibernateException
	 */
	void removeCollection(CollectionPersister role, Serializable collectionKey, EventSource source) throws HibernateException {
		if ( log.isTraceEnabled() ) {
			log.trace(
					"collection dereferenced while transient " +
					MessageHelper.collectionInfoString( role, ownerIdentifier, source.getFactory() )
			);
		}
		source.getActionQueue().addAction( new CollectionRemoveAction( null, role, collectionKey, false, source ) );
	}

	/**
	 * This version is slightly different for say
	 * {@link org.hibernate.type.CollectionType#getKeyOfOwner} in that here we
	 * need to assume that the owner is not yet associated with the session,
	 * and thus we cannot rely on the owner's EntityEntry snapshot...
	 *
	 * @param role The persister for the collection role being processed.
	 * @return
	 */
	final Serializable extractCollectionKeyFromOwner(CollectionPersister role) {
		if ( role.getCollectionType().useLHSPrimaryKey() ) {
			return ownerIdentifier;
		}
		else {
			return ( Serializable ) role.getOwnerEntityPersister().getPropertyValue( owner, role.getCollectionType().getLHSPropertyName(), getSession().getEntityMode() );
		}

	}
}
