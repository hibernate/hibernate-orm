/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.function.Supplier;

import org.hibernate.annotations.OrderBy;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Set;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.usertype.UserCollectionType;

/**
 * A {@link CollectionBinder} for {@link org.hibernate.collection.spi.PersistentSet sets},
 * whose mapping model type is {@link Set}.
 *
 * @author Matthew Inger
 */
public class SetBinder extends CollectionBinder {

	public SetBinder(
			Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver,
			boolean sorted,
			MetadataBuildingContext buildingContext) {
		super( customTypeBeanResolver, sorted, buildingContext );
	}

	@Override
	protected Collection createCollection(PersistentClass persistentClass) {
		return new Set( getCustomTypeBeanResolver(), persistentClass, getBuildingContext() );
	}

	@Override
	public void setSqlOrderBy(OrderBy orderByAnn) {
		if ( orderByAnn != null ) {
			super.setSqlOrderBy( orderByAnn );
		}
	}
}
