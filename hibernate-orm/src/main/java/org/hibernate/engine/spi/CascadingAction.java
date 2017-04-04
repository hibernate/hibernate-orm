/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

/**
 * A session action that may be cascaded from parent entity to its children
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface CascadingAction {

	/**
	 * Cascade the action to the child object.
	 *
	 * @param session The session within which the cascade is occuring.
	 * @param child The child to which cascading should be performed.
	 * @param entityName The child's entity name
	 * @param anything Anything ;)  Typically some form of cascade-local cache
	 * which is specific to each CascadingAction type
	 * @param isCascadeDeleteEnabled Are cascading deletes enabled.
	 * @throws HibernateException
	 */
	void cascade(
			EventSource session,
			Object child,
			String entityName,
			Object anything,
			boolean isCascadeDeleteEnabled) throws HibernateException;

	/**
	 * Given a collection, get an iterator of the children upon which the
	 * current cascading action should be visited.
	 *
	 * @param session The session within which the cascade is occuring.
	 * @param collectionType The mapping type of the collection.
	 * @param collection The collection instance.
	 * @return The children iterator.
	 */
	Iterator getCascadableChildrenIterator(
			EventSource session,
			CollectionType collectionType,
			Object collection);

	/**
	 * Does this action potentially extrapolate to orphan deletes?
	 *
	 * @return True if this action can lead to deletions of orphans.
	 */
	boolean deleteOrphans();


	/**
	 * Does the specified cascading action require verification of no cascade validity?
	 *
	 * @return True if this action requires no-cascade verification; false otherwise.
	 */
	boolean requiresNoCascadeChecking();

	/**
	 * Called (in the case of {@link #requiresNoCascadeChecking} returning true) to validate
	 * that no cascade on the given property is considered a valid semantic.
	 *
	 * @param session The session witin which the cascade is occurring.
	 * @param parent The property value owner
	 * @param persister The entity persister for the owner
	 * @param propertyType The property type
	 * @param propertyIndex The index of the property within the owner.
	 */
	void noCascade(EventSource session, Object parent, EntityPersister persister, Type propertyType, int propertyIndex);

	/**
	 * Should this action be performed (or noCascade consulted) in the case of lazy properties.
	 */
	boolean performOnLazyProperty();
}
