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
 * @author Gail Badner
 */
public interface LoadQueryAliasResolutionContext {

	public String resolveEntityReturnAlias(EntityReturn entityReturn);

	public String resolveCollectionReturnAlias(CollectionReturn collectionReturn);

	String[] resolveScalarReturnAliases(ScalarReturn scalarReturn);

	/**
	 * Retrieve the SQL table alias.
	 *
	 * @return The SQL table alias
	 */
	String resolveEntitySqlTableAlias(EntityReference entityReference);

	EntityAliases resolveEntityColumnAliases(EntityReference entityReference);

	String resolveCollectionSqlTableAlias(CollectionReference collectionReference);

	/**
	 * Returns the description of the aliases in the JDBC ResultSet that identify values "belonging" to the
	 * this collection.
	 *
	 * @return The ResultSet alias descriptor for the collection
	 */
	CollectionAliases resolveCollectionColumnAliases(CollectionReference collectionReference);

	/**
	 * If the elements of this collection are entities, this methods returns the JDBC ResultSet alias descriptions
	 * for that entity; {@code null} indicates a non-entity collection.
	 *
	 * @return The ResultSet alias descriptor for the collection's entity element, or {@code null}
	 */
	EntityAliases resolveCollectionElementColumnAliases(CollectionReference collectionReference);

	String resolveRhsAlias(JoinableAssociation joinableAssociation);

	String resolveLhsAlias(JoinableAssociation joinableAssociation);

	String[] resolveAliasedLhsColumnNames(JoinableAssociation joinableAssociation);

	EntityAliases resolveCurrentEntityAliases(JoinableAssociation joinableAssociation);

	CollectionAliases resolveCurrentCollectionAliases(JoinableAssociation joinableAssociation);
}
