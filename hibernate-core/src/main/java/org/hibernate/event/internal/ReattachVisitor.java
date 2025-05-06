/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

import static org.hibernate.pretty.MessageHelper.collectionInfoString;

/**
 * Abstract superclass of visitors that reattach collections.
 *
 * @author Gavin King
 */
public abstract class ReattachVisitor extends ProxyVisitor {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( ReattachVisitor.class );

	private final Object ownerIdentifier;
	private final Object owner;

	public ReattachVisitor(EventSource session, Object ownerIdentifier, Object owner) {
		super( session );
		this.ownerIdentifier = ownerIdentifier;
		this.owner = owner;
	}

	/**
	 * Retrieve the identifier of the entity being visited.
	 *
	 * @return The entity's identifier.
	 */
	final Object getOwnerIdentifier() {
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
	@Override
	Object processComponent(Object component, CompositeType componentType) throws HibernateException {
		final Type[] types = componentType.getSubtypes();
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
	 *
	 */
	void removeCollection(CollectionPersister role, Object collectionKey, EventSource source)
			throws HibernateException {
		if ( LOG.isTraceEnabled() ) {
			LOG.trace( "Collection dereferenced while transient "
						+ collectionInfoString( role, ownerIdentifier, source.getFactory() ) );
		}
		source.getActionQueue().addAction( new CollectionRemoveAction( owner, role, collectionKey, false, source ) );
	}

	/**
	 * This version is slightly different for say
	 * {@link org.hibernate.type.CollectionType#getKeyOfOwner} in that here we
	 * need to assume that the owner is not yet associated with the session,
	 * and thus we cannot rely on the owner's EntityEntry snapshot...
	 *
	 * @param role The persister for the collection role being processed.
	 *
	 * @return The value from the owner that identifies the grouping into the collection
	 */
	final Object extractCollectionKeyFromOwner(CollectionPersister role) {
		final CollectionType collectionType = role.getCollectionType();
		if ( collectionType.useLHSPrimaryKey() ) {
			return ownerIdentifier;
		}
		return role.getOwnerEntityPersister()
				.getPropertyValue( owner, collectionType.getLHSPropertyName() );
	}
}
