/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.plan.exec.spi;

import org.hibernate.loader.CollectionAliases;

/**
 * @author Steve Ebersole
 */
public interface CollectionReferenceAliases {
	/**
	 * Obtain the table alias used for the collection table of the CollectionReference.
	 *
	 * @return The collection table alias.
	 */
	public String getCollectionTableAlias();

	/**
	 * Obtain the alias of the table that contains the collection element values.
	 * <p/>
	 * Unlike in the legacy Loader case, CollectionReferences in the LoadPlan code refer to both the
	 * collection and the elements *always*.  In Loader the elements were handled by EntityPersister associations
	 * entries for one-to-many and many-to-many.  In LoadPlan we need to describe the collection table/columns
	 * as well as the entity element table/columns.  For "basic collections" and one-to-many collections, the
	 * "element table" and the "collection table" are actually the same.  For the many-to-many case this will be
	 * different and we need to track it separately.
	 *
	 * @return The element table alias.  Only different from {@link #getCollectionTableAlias()} in the case of
	 * many-to-many.
	 */
	public String getElementTableAlias();

	/**
	 * Obtain the aliases for the columns related to the collection structure such as the FK, index/key, or identifier
	 * (idbag).
	 *
	 * @return The collection column aliases.
	 */
	public CollectionAliases getCollectionColumnAliases();

	/**
	 * Obtain the entity reference aliases for the element values when the element of the collection is an entity.
	 *
	 * @return The entity reference aliases for the entity element; {@code null} if the collection element is not an entity.
	 */
	public EntityReferenceAliases getEntityElementAliases();
}
