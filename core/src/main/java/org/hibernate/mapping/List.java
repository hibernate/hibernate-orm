//$Id: List.java 5793 2005-02-20 03:34:50Z oneovthafew $
package org.hibernate.mapping;

import org.hibernate.MappingException;
import org.hibernate.type.CollectionType;
import org.hibernate.type.TypeFactory;

/**
 * A list mapping has a primary key consisting of
 * the key columns + index column.
 * @author Gavin King
 */
public class List extends IndexedCollection {
	
	private int baseIndex;

	public boolean isList() {
		return true;
	}

	public List(PersistentClass owner) {
		super(owner);
	}

	public CollectionType getDefaultCollectionType() throws MappingException {
		return TypeFactory.list( getRole(), getReferencedPropertyName(), isEmbedded() );
	}
	
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	public int getBaseIndex() {
		return baseIndex;
	}
	
	public void setBaseIndex(int baseIndex) {
		this.baseIndex = baseIndex;
	}
}
