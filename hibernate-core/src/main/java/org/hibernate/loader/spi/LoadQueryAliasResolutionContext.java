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
package org.hibernate.loader.spi;

import org.hibernate.loader.CollectionAliases;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.CollectionReference;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.ScalarReturn;

/**
 * Provides aliases that are used by load queries and ResultSet processors.
 *
 * @author Gail Badner
 */
public interface LoadQueryAliasResolutionContext {

	/**
	 * Resolve the alias associated with the specified {@link EntityReturn}.
	 *
	 * @param entityReturn - the {@link EntityReturn}.
	 *
	 * @return the alias associated with the specified {@link EntityReturn}.
	 */
	public String resolveEntityReturnAlias(EntityReturn entityReturn);

	/**
	 * Resolve the alias associated with the specified {@link CollectionReturn}.
	 *
	 * @param collectionReturn - the {@link CollectionReturn}.
	 *
	 * @return the alias associated with {@link CollectionReturn}.
	 */
	public String resolveCollectionReturnAlias(CollectionReturn collectionReturn);

	/**
	 * Resolve the aliases associated with the specified {@link ScalarReturn}.
	 *
	 * @param scalarReturn - the {@link ScalarReturn}.
	 *
	 * @return the alias associated with {@link ScalarReturn}.
	 */
	String[] resolveScalarReturnAliases(ScalarReturn scalarReturn);

	/**
	 * Resolve the SQL table alias for the specified {@link EntityReference}.
	 *
	 * @param entityReference - the {@link EntityReference}.
	 * @return The SQL table alias for the specified {@link EntityReference}.
	 */
	String resolveEntityTableAlias(EntityReference entityReference);

	/**
	 * Returns the description of the aliases in the JDBC ResultSet that identify values "belonging" to
	 * an entity.
	 *
	 * @param entityReference - the {@link EntityReference} for the entity.
	 *
	 * @return The ResultSet alias descriptor for the {@link EntityReference}
	 */
	EntityAliases resolveEntityColumnAliases(EntityReference entityReference);

	/**
	 * Resolve the SQL table alias for the specified {@link CollectionReference}.
	 *
	 * @param collectionReference - the {@link CollectionReference}.
	 * @return The SQL table alias for the specified {@link CollectionReference}.
	 */
	String resolveCollectionTableAlias(CollectionReference collectionReference);

	/**
	 * Returns the description of the aliases in the JDBC ResultSet that identify values "belonging" to
	 * the specified {@link CollectionReference}.
	 *
	 * @return The ResultSet alias descriptor for the {@link CollectionReference}
	 */
	CollectionAliases resolveCollectionColumnAliases(CollectionReference collectionReference);

	/**
	 * If the elements of this collection are entities, this methods returns the JDBC ResultSet alias descriptions
	 * for that entity; {@code null} indicates a non-entity collection.
	 *
	 * @return The ResultSet alias descriptor for the collection's entity element, or {@code null}
	 */
	EntityAliases resolveCollectionElementColumnAliases(CollectionReference collectionReference);

	/**
	 * Resolve the table alias on the right-hand-side of the specified association.
	 *
	 * @param association - the joinable association.
	 *
	 * @return the table alias on the right-hand-side of the specified association.
	 */
	String resolveAssociationRhsTableAlias(JoinableAssociation association);

	/**
	 * Resolve the table alias on the left-hand-side of the specified association.
	 *
	 * @param association - the joinable association.
	 *
	 * @return the table alias on the left-hand-side of the specified association.
	 */
	String resolveAssociationLhsTableAlias(JoinableAssociation association);

	/**
	 * Resolve the column aliases on the left-hand-side of the specified association.
	 * @param association - the joinable association
	 * @return the column aliases on the left-hand-side of the specified association.
	 */
	String[] resolveAssociationAliasedLhsColumnNames(JoinableAssociation association);
}
