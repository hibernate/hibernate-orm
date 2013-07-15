/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.spi;

import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CollectionType;

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
	public void cascade(
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
	public Iterator getCascadableChildrenIterator(
			EventSource session,
			CollectionType collectionType,
			Object collection);

	/**
	 * Does this action potentially extrapolate to orphan deletes?
	 *
	 * @return True if this action can lead to deletions of orphans.
	 */
	public boolean deleteOrphans();


	/**
	 * Does the specified cascading action require verification of no cascade validity?
	 *
	 * @return True if this action requires no-cascade verification; false otherwise.
	 */
	public boolean requiresNoCascadeChecking();

	/**
	 * Called (in the case of {@link #requiresNoCascadeChecking} returning true) to validate
	 * that no cascade on the given property is considered a valid semantic.
	 *
	 * @param session The session witin which the cascade is occurring.
	 * @param child The property value
	 * @param parent The property value owner
	 * @param persister The entity persister for the owner
	 * @param propertyIndex The index of the property within the owner.
	 */
	public void noCascade(EventSource session, Object child, Object parent, EntityPersister persister, int propertyIndex);

	/**
	 * Should this action be performed (or noCascade consulted) in the case of lazy properties.
	 */
	public boolean performOnLazyProperty();
}
