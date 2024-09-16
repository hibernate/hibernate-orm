/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.CollectionType;
import org.hibernate.type.MapType;
import org.hibernate.type.OrderedMapType;
import org.hibernate.type.SortedMapType;
import org.hibernate.usertype.UserCollectionType;

/**
 * A mapping model object representing a collection of type {@link java.util.Map}.
 * A map has a primary key consisting of the key columns + index columns.
 */
public class Map extends IndexedCollection {

	private String mapKeyPropertyName;

	public Map(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );
	}

	public Map(Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver, PersistentClass owner, MetadataBuildingContext buildingContext) {
		super( customTypeBeanResolver, owner, buildingContext );
	}

	private Map(Map original) {
		super( original );
	}

	@Override
	public Map copy() {
		return new Map( this );
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

	public CollectionType getDefaultCollectionType() {
		if ( isSorted() ) {
			return new SortedMapType( getRole(), getReferencedPropertyName(), getComparator() );
		}

		if ( hasOrder() ) {
			return new OrderedMapType( getRole(), getReferencedPropertyName() );
		}

		return new MapType( getRole(), getReferencedPropertyName() );
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
