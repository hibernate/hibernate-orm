/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.util.Iterator;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.Type;

/**
 * A session action that may be cascaded from parent entity to its children
 *
 * @param <T> The type of some context propagated with the cascading action
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
	 * @param anything Some context specific to the kind of {@link CascadingAction}
	 * @param isCascadeDeleteEnabled Whether the foreign key is declared with
	 *        {@link org.hibernate.annotations.OnDeleteAction#CASCADE on delete cascade}.
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
	 * Does the specified cascading action require verification of no cascade validity?
	 *
	 * @return True if this action requires no-cascade verification; false otherwise.
	 *
	 * @deprecated No longer used
	 */
	@Deprecated(since = "6.6", forRemoval = true)
	default boolean requiresNoCascadeChecking() {
		return false;
	}

	/**
	 * Called (in the case of {@link #requiresNoCascadeChecking} returning true) to validate
	 * that no cascade on the given property is considered a valid semantic.
	 *
	 * @param session The session within which the cascade is occurring.
	 * @param parent The property value owner
	 * @param persister The entity persister for the owner
	 * @param propertyType The property type
	 * @param propertyIndex The index of the property within the owner.
	 *
	 * @deprecated No longer used
	 */
	@Deprecated(since = "6.6", forRemoval = true)
	default void noCascade(EventSource session, Object parent, EntityPersister persister, Type propertyType, int propertyIndex) {}

	/**
	 * Should this action be performed (or noCascade consulted) in the case of lazy properties.
	 */
	boolean performOnLazyProperty();

	/**
	 * The cascade direction in which we care whether the foreign key is declared with
	 * {@link org.hibernate.annotations.OnDeleteAction#CASCADE on delete cascade}.
	 *
	 * @apiNote This allows us to reuse the long-existing boolean parameter of
	 *          {@link #cascade(EventSource, Object, String, Object, boolean)}
	 *          for multiple purposes.
	 *
	 */
	@Incubating @Nullable
	default ForeignKeyDirection directionAffectedByCascadeDelete(){
		return null;
	}
}
