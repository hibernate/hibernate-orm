/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.util.Iterator;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.internal.CascadePoint;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.AssociationType;
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
	 * @param childEntityName The name of the child entity
	 * @param parentEntityName The name of the parent entity
	 * @param propertyName The name of the attribute of the parent entity being cascaded
	 * @param attributePath The full path of the attribute of the parent entity being cascaded
	 * @param anything Anything ;) Typically some form of cascade-local cache
	 *                 which is specific to each {@link CascadingAction} type
	 * @param isCascadeDeleteEnabled Are cascading deletes enabled.
	 */
	void cascade(
			EventSource session,
			Object child,
			String childEntityName,
			String parentEntityName,
			String propertyName,
			@Nullable List<String> attributePath,
			T anything,
			boolean isCascadeDeleteEnabled);

	/**
	 * @deprecated No longer called. Will be removed.
	 */
	@Deprecated(since = "7", forRemoval = true)
	default void cascade(
			EventSource session,
			Object child,
			String childEntityName,
			T anything,
			boolean isCascadeDeleteEnabled) {
		cascade( session, child, childEntityName,
				"", "", null,
				anything, isCascadeDeleteEnabled );
	}

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

	/**
	 * Does this action cascade to the given association at the given {@link CascadePoint}?
	 *
	 * @since 7
	 */
	boolean cascadeNow(
			CascadePoint cascadePoint,
			AssociationType associationType,
			SessionFactoryImplementor factory);
}
