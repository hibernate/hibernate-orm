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
 * A map has a primary key consisting of
 * the key columns + index columns.
 */
public class Map extends IndexedCollection {

	/**
	 * @deprecated Use {@link Map#Map(MetadataBuildingContext, PersistentClass)} instead.
	 */
	@Deprecated
	public Map(MetadataImplementor metadata, PersistentClass owner) {
		super( metadata, owner );
	}

	public Map(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );
	}
	
	public boolean isMap() {
		return true;
	}

	public CollectionType getDefaultCollectionType() {
		if ( isSorted() ) {
			return getMetadata().getTypeResolver()
					.getTypeFactory()
					.sortedMap( getRole(), getReferencedPropertyName(), getComparator() );
		}
		else if ( hasOrder() ) {
			return getMetadata().getTypeResolver()
					.getTypeFactory()
					.orderedMap( getRole(), getReferencedPropertyName() );
		}
		else {
			return getMetadata().getTypeResolver()
					.getTypeFactory()
					.map( getRole(), getReferencedPropertyName() );
		}
	}


	public void createAllKeys() throws MappingException {
		super.createAllKeys();
		if ( !isInverse() ) {
			getIndex().createForeignKey();
		}
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
}
