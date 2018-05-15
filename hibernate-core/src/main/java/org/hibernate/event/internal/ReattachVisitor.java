/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.io.Serializable;
import java.util.Collection;

import org.hibernate.HibernateException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.pretty.MessageHelper;

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
	Object processComponent(Object component, EmbeddedTypeDescriptor descriptor) throws HibernateException {
		final Collection subclassTypes = descriptor.getSubclassTypes();
		if ( subclassTypes == null ) {
			processValues( new Object[subclassTypes.size()], subclassTypes );
		}
		else {
			super.processComponent( component, descriptor );
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
	void removeCollection(PersistentCollectionDescriptor role, Serializable collectionKey, EventSource source)
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
	 * This version is slightly different here we
	 * need to assume that the owner is not yet associated with the session,
	 * and thus we cannot rely on the owner's EntityEntry snapshot...
	 *
	 * @param collectionDescriptor The descriptor for the collection being processed.
	 *
	 * @return The value from the owner that identifies the grouping into the collection
	 */
	final Serializable extractCollectionKeyFromOwner(PersistentCollectionDescriptor collectionDescriptor) {
		throw new NotYetImplementedFor6Exception(  );
//		if ( collectionDescriptor.getCollectionType().useLHSPrimaryKey() ) {
//			return ownerIdentifier;
//		}
//		return (Serializable) collectionDescriptor.getOwnerEntityPersister().getPropertyValue(
//				owner,
//				collectionDescriptor.getCollectionType().getLHSPropertyName()
//		);
	}
}
