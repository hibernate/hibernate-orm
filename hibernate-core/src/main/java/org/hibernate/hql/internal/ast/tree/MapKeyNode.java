/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Inc. or third-party contributors as
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
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.type.Type;

/**
 * Tree node representing reference to the key of a Map association.
 *
 * @author Steve Ebersole
 */
public class MapKeyNode extends AbstractMapComponentNode {
	@Override
	protected String expressionDescription() {
		return "key(*)";
	}

	@Override
	protected String[] resolveColumns(QueryableCollection collectionPersister) {
		final FromElement fromElement = getFromElement();
		return fromElement.toColumns(
				fromElement.getCollectionTableAlias(),
				"index", // the JPA KEY "qualifier" is the same concept as the HQL INDEX function/property
				getWalker().isInSelect()
		);
	}

	@Override
	protected Type resolveType(QueryableCollection collectionPersister) {
		return collectionPersister.getIndexType();
	}
}
