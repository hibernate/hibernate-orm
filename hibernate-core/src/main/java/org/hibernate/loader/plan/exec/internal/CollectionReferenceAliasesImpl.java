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
