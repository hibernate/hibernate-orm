/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.function.Supplier;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Array;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.usertype.UserCollectionType;

/**
 * A {@link CollectionBinder} for {@link org.hibernate.collection.spi.PersistentArrayHolder primitive arrays},
 * whose mapping model type is {@link org.hibernate.mapping.Array}.
 *
 * @author Anthony Patricio
 */
public class ArrayBinder extends ListBinder {

	public ArrayBinder(
			Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver,
			MetadataBuildingContext buildingContext) {
		super( customTypeBeanResolver, buildingContext );
	}

	@Override
	protected Collection createCollection(PersistentClass owner) {
		return new Array( getCustomTypeBeanResolver(), owner, getBuildingContext() );
	}
}
