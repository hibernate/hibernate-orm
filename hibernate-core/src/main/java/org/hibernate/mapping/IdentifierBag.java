/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.function.Supplier;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.CollectionType;
import org.hibernate.type.IdentifierBagType;
import org.hibernate.usertype.UserCollectionType;

/**
 * A bag with a generated (surrogate) key. Its primary key is just the identifier column.
 */
public class IdentifierBag extends IdentifierCollection {

	/**
	 * hbm.xml binding
	 */
	public IdentifierBag(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );
	}

	/**
	 * annotation binding
	 */
	public IdentifierBag(
			Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver, PersistentClass owner,
			MetadataBuildingContext buildingContext) {
		super( customTypeBeanResolver, owner, buildingContext );
	}

	public IdentifierBag(IdentifierBag original) {
		super( original );
	}

	@Override
	public IdentifierBag copy() {
		return new IdentifierBag( this );
	}

	public CollectionType getDefaultCollectionType() {
		return new IdentifierBagType( getRole(), getReferencedPropertyName() );
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
}
