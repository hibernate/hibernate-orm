/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.collection.internal.StandardMapSemantics;
import org.hibernate.collection.internal.StandardOrderedMapSemantics;
import org.hibernate.collection.internal.StandardSortedMapSemantics;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.type.CollectionType;
import org.hibernate.type.MapType;
import org.hibernate.type.OrderedMapType;
import org.hibernate.type.SortedMapType;

/**
 * A map has a primary key consisting of
 * the key columns + index columns.
 */
public class Map extends IndexedCollection {

	private String mapKeyPropertyName;

	public Map(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );
	}

	public Map(SemanticsResolver semanticsResolver, PersistentClass owner, MetadataBuildingContext buildingContext) {
		super( semanticsResolver, owner, buildingContext );
	}

	public boolean isMap() {
		return true;
	}

	public String getMapKeyPropertyName() {
		return mapKeyPropertyName;
	}

	public void setMapKeyPropertyName(String mapKeyPropertyName) {
		this.mapKeyPropertyName = mapKeyPropertyName;
	}

	@Override
	public CollectionSemantics getDefaultCollectionSemantics() {
		if ( isSorted() ) {
			return StandardSortedMapSemantics.INSTANCE;
		}

		if ( hasOrder() ) {
			return StandardOrderedMapSemantics.INSTANCE;
		}

		return StandardMapSemantics.INSTANCE;
	}

	public CollectionType getDefaultCollectionType() {
		if ( isSorted() ) {
			return new SortedMapType( getTypeConfiguration(), getRole(), getReferencedPropertyName(), getComparator() );
		}

		if ( hasOrder() ) {
			return new OrderedMapType( getTypeConfiguration(), getRole(), getReferencedPropertyName() );
		}

		return new MapType( getTypeConfiguration(), getRole(), getReferencedPropertyName() );
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
