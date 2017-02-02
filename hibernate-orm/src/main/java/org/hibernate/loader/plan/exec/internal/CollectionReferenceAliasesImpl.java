/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.internal;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.loader.plan.exec.spi.CollectionReferenceAliases;
import org.hibernate.loader.plan.exec.spi.EntityReferenceAliases;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class CollectionReferenceAliasesImpl implements CollectionReferenceAliases {
	private final String tableAlias;
	private final String manyToManyAssociationTableAlias;
	private final CollectionAliases collectionAliases;
	private final EntityReferenceAliases entityElementAliases;

	public CollectionReferenceAliasesImpl(
			String tableAlias,
			String manyToManyAssociationTableAlias,
			CollectionAliases collectionAliases,
			EntityReferenceAliases entityElementAliases) {
		this.tableAlias = tableAlias;
		this.manyToManyAssociationTableAlias = manyToManyAssociationTableAlias;
		this.collectionAliases = collectionAliases;
		this.entityElementAliases = entityElementAliases;
	}

	@Override
	public String getCollectionTableAlias() {
		return StringHelper.isNotEmpty( manyToManyAssociationTableAlias )
				? manyToManyAssociationTableAlias
				: tableAlias;
	}

	@Override
	public String getElementTableAlias() {
		return tableAlias;
	}

	@Override
	public CollectionAliases getCollectionColumnAliases() {
		return collectionAliases;
	}

	@Override
	public EntityReferenceAliases getEntityElementAliases() {
		return entityElementAliases;
	}
}
