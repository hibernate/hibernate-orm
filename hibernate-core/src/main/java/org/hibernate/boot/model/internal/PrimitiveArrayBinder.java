/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.function.Supplier;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PrimitiveArray;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.usertype.UserCollectionType;

/**
 * A {@link CollectionBinder} for {@link org.hibernate.collection.spi.PersistentArrayHolder primitive arrays},
 * whose mapping model type is {@link org.hibernate.mapping.PrimitiveArray}.
 *
 * @author Emmanuel Bernard
 */
public class PrimitiveArrayBinder extends ArrayBinder {
	public PrimitiveArrayBinder(Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver, MetadataBuildingContext buildingContext) {
		super( customTypeBeanResolver, buildingContext );
	}

	@Override
	protected Collection createCollection(PersistentClass owner) {
		return new PrimitiveArray( getCustomTypeBeanResolver(), owner, getBuildingContext() );
	}
}
