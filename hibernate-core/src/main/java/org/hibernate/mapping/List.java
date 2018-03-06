/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.type.CollectionType;

/**
 * A list mapping has a primary key consisting of the key columns + index column.
 *
 * @author Gavin King
 */
public class List extends IndexedCollection {
	
	private int baseIndex;

	public boolean isList() {
		return true;
	}

	/**
	 * @deprecated Use {@link List#List(MetadataBuildingContext, PersistentClass)} instead.
	 */
	@Deprecated
	public List(MetadataImplementor metadata, PersistentClass owner) {
		super( metadata, owner );
	}

	public List(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );
	}

	public CollectionType getDefaultCollectionType() throws MappingException {
		return getMetadata().getTypeResolver()
				.getTypeFactory()
				.list( getRole(), getReferencedPropertyName() );
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
