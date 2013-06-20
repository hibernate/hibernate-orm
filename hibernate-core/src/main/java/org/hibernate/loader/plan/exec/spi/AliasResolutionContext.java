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
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.plan.spi.CollectionReference;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.Return;
import org.hibernate.loader.plan.spi.ScalarReturn;
import org.hibernate.loader.spi.JoinableAssociation;

/**
 * Provides aliases that are used by load queries and ResultSet processors.
 *
 * @author Gail Badner
 * @author Steve Ebersole
 */
public interface AliasResolutionContext {
	/**
	 * Resolve the source alias (select-clause assigned alias) associated with the specified Return.  The source
	 * alias is the alias associated with the Return in the source query.
	 * <p/>
	 * The concept of a source alias only has meaning in the case of queries (HQL, Criteria, etc).  Not sure we
	 * really need to keep these here.  One argument for keeping them is that I always thought it would be nice to
	 * base the SQL aliases on the source aliases.  Keeping the source aliases here would allow us to do that as
	 * we are generating those SQL aliases internally.
	 * <p/>
	 * Should also consider pushing the source "from clause aliases" here if we keep pushing the select aliases
	 *
	 * @param theReturn The Return to locate
	 *
	 * @return the alias associated with the specified {@link EntityReturn}.
	 */
	public String getSourceAlias(Return theReturn);

	/**
	 * Resolve the SQL column aliases associated with the specified {@link ScalarReturn}.
	 *
	 * @param scalarReturn The {@link ScalarReturn} for which we want SQL column aliases
	 *
	 * @return The SQL column aliases associated with {@link ScalarReturn}.
	 */
	public String[] resolveScalarColumnAliases(ScalarReturn scalarReturn);

	/**
	 * Resolve the alias information related to the given entity reference.
	 *
	 * @param entityReference The entity reference for which to obtain alias info
	 *
	 * @return The resolved alias info,
	 */
	public EntityReferenceAliases resolveAliases(EntityReference entityReference);

	/**
	 * Resolve the alias information related to the given collection reference.
	 *
	 * @param collectionReference The collection reference for which to obtain alias info
	 *
	 * @return The resolved alias info,
	 */
	public CollectionReferenceAliases resolveAliases(CollectionReference collectionReference);





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
