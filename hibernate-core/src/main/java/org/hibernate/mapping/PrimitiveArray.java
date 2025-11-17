/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.function.Supplier;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.usertype.UserCollectionType;

/**
 * A primitive array has a primary key consisting of the key columns + index column.
 */
public class PrimitiveArray extends Array {
	public PrimitiveArray(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );
	}

	public PrimitiveArray(Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver, PersistentClass owner, MetadataBuildingContext buildingContext) {
		super( customTypeBeanResolver, owner, buildingContext );
	}

	private PrimitiveArray(PrimitiveArray original) {
		super( original );
	}

	@Override
	public Array copy() {
		return new PrimitiveArray( this );
	}

	public boolean isPrimitiveArray() {
		return true;
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
}
