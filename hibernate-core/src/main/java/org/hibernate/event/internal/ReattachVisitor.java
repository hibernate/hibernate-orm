/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.event.internal;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * Abstract superclass of visitors that reattach collections.
 *
 * @author Gavin King
 */
public abstract class ReattachVisitor extends ProxyVisitor {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( ReattachVisitor.class );

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
	@Override
	Object processComponent(Object component, CompositeType componentType) throws HibernateException {
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
	 *
	 * @throws HibernateException
	 */
	void removeCollection(CollectionPersister role, Serializable collectionKey, EventSource source)
			throws HibernateException {
		if ( LOG.isTraceEnabled() ) {
			LOG.tracev(
					"Collection dereferenced while transient {0}",
					MessageHelper.collectionInfoString( role, ownerIdentifier, source.getFactory() )
			);
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
	final Serializable extractCollectionKeyFromOwner(CollectionPersister role) {
		if ( role.getCollectionType().useLHSPrimaryKey() ) {
			return ownerIdentifier;
		}
		return (Serializable) role.getOwnerEntityPersister().getPropertyValue(
				owner,
				role.getCollectionType().getLHSPropertyName()
		);
	}
}
