//$Id: Map.java 7714 2005-08-01 16:29:33Z oneovthafew $
package org.hibernate.mapping;

import org.hibernate.MappingException;
import org.hibernate.type.CollectionType;
import org.hibernate.type.TypeFactory;

/**
 * A map has a primary key consisting of
 * the key columns + index columns.
 */
public class Map extends IndexedCollection {

	public Map(PersistentClass owner) {
		super(owner);
	}
	
	public boolean isMap() {
		return true;
	}

	public CollectionType getDefaultCollectionType() {
		if ( isSorted() ) {
			return TypeFactory.sortedMap( getRole(), getReferencedPropertyName(), isEmbedded(), getComparator() );
		}
		else if ( hasOrder() ) {
			return TypeFactory.orderedMap( getRole(), getReferencedPropertyName(), isEmbedded() );
		}
		else {
			return TypeFactory.map( getRole(), getReferencedPropertyName(), isEmbedded() );
		}
	}


	public void createAllKeys() throws MappingException {
		super.createAllKeys();
		if ( !isInverse() ) getIndex().createForeignKey();
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
}
