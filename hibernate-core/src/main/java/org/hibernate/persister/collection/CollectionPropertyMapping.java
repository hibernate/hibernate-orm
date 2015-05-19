/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection;

import org.hibernate.QueryException;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * @author Gavin King
 */
public class CollectionPropertyMapping implements PropertyMapping {
	private final QueryableCollection memberPersister;

	public CollectionPropertyMapping(QueryableCollection memberPersister) {
		this.memberPersister = memberPersister;
	}

	public Type toType(String propertyName) throws QueryException {
		if ( propertyName.equals(CollectionPropertyNames.COLLECTION_ELEMENTS) ) {
			return memberPersister.getElementType();
		}
		else if ( propertyName.equals(CollectionPropertyNames.COLLECTION_INDICES) ) {
			if ( !memberPersister.hasIndex() ) {
				throw new QueryException("unindexed collection before indices()");
			}
			return memberPersister.getIndexType();
		}
		else if ( propertyName.equals(CollectionPropertyNames.COLLECTION_SIZE) ) {
			return StandardBasicTypes.INTEGER;
		}
		else if ( propertyName.equals(CollectionPropertyNames.COLLECTION_MAX_INDEX) ) {
			return memberPersister.getIndexType();
		}
		else if ( propertyName.equals(CollectionPropertyNames.COLLECTION_MIN_INDEX) ) {
			return memberPersister.getIndexType();
		}
		else if ( propertyName.equals(CollectionPropertyNames.COLLECTION_MAX_ELEMENT) ) {
			return memberPersister.getElementType();
		}
		else if ( propertyName.equals(CollectionPropertyNames.COLLECTION_MIN_ELEMENT) ) {
			return memberPersister.getElementType();
		}
		else {
			//return memberPersister.getPropertyType(propertyName);
			throw new QueryException("illegal syntax near collection: " + propertyName);
		}
	}

	public String[] toColumns(String alias, String propertyName) throws QueryException {
		if ( propertyName.equals(CollectionPropertyNames.COLLECTION_ELEMENTS) ) {
			return memberPersister.getElementColumnNames(alias);
		}
		else if ( propertyName.equals(CollectionPropertyNames.COLLECTION_INDICES) ) {
			if ( !memberPersister.hasIndex() ) {
				throw new QueryException("unindexed collection in indices()");
			}
			return memberPersister.getIndexColumnNames(alias);
		}
		else if ( propertyName.equals(CollectionPropertyNames.COLLECTION_SIZE) ) {
			String[] cols = memberPersister.getKeyColumnNames();
			return new String[] { "count(" + alias + '.' + cols[0] + ')' };
		}
		else if ( propertyName.equals(CollectionPropertyNames.COLLECTION_MAX_INDEX) ) {
			if ( !memberPersister.hasIndex() ) {
				throw new QueryException("unindexed collection in maxIndex()");
			}
			String[] cols = memberPersister.getIndexColumnNames(alias);
			if ( cols.length!=1 ) {
				throw new QueryException("composite collection index in maxIndex()");
			}
			return new String[] { "max(" + cols[0] + ')' };
		}
		else if ( propertyName.equals(CollectionPropertyNames.COLLECTION_MIN_INDEX) ) {
			if ( !memberPersister.hasIndex() ) {
				throw new QueryException("unindexed collection in minIndex()");
			}
			String[] cols = memberPersister.getIndexColumnNames(alias);
			if ( cols.length!=1 ) {
				throw new QueryException("composite collection index in minIndex()");
			}
			return new String[] { "min(" + cols[0] + ')' };
		}
		else if ( propertyName.equals(CollectionPropertyNames.COLLECTION_MAX_ELEMENT) ) {
			String[] cols = memberPersister.getElementColumnNames(alias);
			if ( cols.length!=1 ) {
				throw new QueryException("composite collection element in maxElement()");
			}
			return new String[] { "max(" + cols[0] + ')' };
		}
		else if ( propertyName.equals(CollectionPropertyNames.COLLECTION_MIN_ELEMENT) ) {
			String[] cols = memberPersister.getElementColumnNames(alias);
			if ( cols.length!=1 ) {
				throw new QueryException("composite collection element in minElement()");
			}
			return new String[] { "min(" + cols[0] + ')' };
		}
		else {
			//return memberPersister.toColumns(alias, propertyName);
			throw new QueryException("illegal syntax near collection: " + propertyName);
		}
	}

	/**
	 * Given a property path, return the corresponding column name(s).
	 */
	public String[] toColumns(String propertyName) throws QueryException, UnsupportedOperationException {
		throw new UnsupportedOperationException( "References to collections must be define a SQL alias" );
	}

	public Type getType() {
		//return memberPersister.getType();
		return memberPersister.getCollectionType();
	}

}
