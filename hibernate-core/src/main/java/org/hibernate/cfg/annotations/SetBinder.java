/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.util.function.Supplier;

import org.hibernate.annotations.OrderBy;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.usertype.UserCollectionType;

/**
 * Bind a set.
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
		return new org.hibernate.mapping.Set( getCustomTypeBeanResolver(), persistentClass, getBuildingContext() );
	}

	@Override
	public void setSqlOrderBy(OrderBy orderByAnn) {
		if ( orderByAnn != null ) {
			super.setSqlOrderBy( orderByAnn );
		}
	}
}
