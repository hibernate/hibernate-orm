/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.type.Type;

/**
 * Tree node representing reference to the value of a Map association.
 *
 * @author Steve Ebersole
 */
public class MapValueNode extends AbstractMapComponentNode {
	@Override
	protected String expressionDescription() {
		return "value(*)";
	}

	@Override
	protected String[] resolveColumns(QueryableCollection collectionPersister) {
		final FromElement fromElement = getFromElement();
		return fromElement.toColumns(
				fromElement.getCollectionTableAlias(),
				"elements", // the JPA VALUE "qualifier" is the same concept as the HQL ELEMENTS function/property
				getWalker().isInSelect()
		);
	}

	@Override
	protected Type resolveType(QueryableCollection collectionPersister) {
		return collectionPersister.getElementType();
	}
}
