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
 * Tree node representing reference to the key of a Map association.
 *
 * @author Steve Ebersole
 */
public class MapKeyNode extends AbstractMapComponentNode {
	private MapKeyEntityFromElement mapKeyEntityFromElement;

	@Override
	protected String expressionDescription() {
		return "key(*)";
	}

	@Override
	protected String[] resolveColumns(QueryableCollection collectionPersister) {
		this.mapKeyEntityFromElement = findOrAddMapKeyEntityFromElement( collectionPersister );
		if ( mapKeyEntityFromElement != null ) {
			setFromElement( mapKeyEntityFromElement );
		}

		final FromElement fromElement = getMapFromElement();
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

	public MapKeyEntityFromElement getMapKeyEntityFromElement() {
		return mapKeyEntityFromElement;
	}
}
