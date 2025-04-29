/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
public interface CascadingAction<T> {

	/**
	 * Cascade the action to the child object.
	 *
	 * @param session The session within which the cascade is occurring.
	 * @param child The child to which cascading should be performed.
	 * @param entityName The child's entity name
	 * @param anything Anything ;)  Typically some form of cascade-local cache
	 * which is specific to each CascadingAction type
	 * @param isCascadeDeleteEnabled Are cascading deletes enabled.
	 */
	void cascade(
			EventSource session,
			Object child,
			String entityName,
			T anything,
			boolean isCascadeDeleteEnabled) throws HibernateException;

	/**
	 * Given a collection, get an iterator of the children upon which the
	 * current cascading action should be visited.
	 *
	 * @param session The session within which the cascade is occurring.
	 * @param collectionType The mapping type of the collection.
	 * @param collection The collection instance.
	 * @return The children iterator.
	 */
	Iterator<?> getCascadableChildrenIterator(
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
	 * Should this action be performed (or noCascade consulted) in the case of lazy properties.
	 */
	boolean performOnLazyProperty();

	/**
	 * Does this action have any work to do for the entity type with the given persister?
	 *
	 * @since 7
	 */
	boolean anythingToCascade(EntityPersister persister);

	/**
	 * Does this action have any work to do for fields of the given type with the given
	 * cascade style?
	 *
	 * @since 7
	 */
	boolean appliesTo(Type type, CascadeStyle style);
}
