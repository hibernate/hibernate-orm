/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.function.Supplier;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.BagType;
import org.hibernate.type.CollectionType;
import org.hibernate.usertype.UserCollectionType;

/**
 * A mapping model object representing a collection of type {@link java.util.Collection} which may
 * contain duplicates. Since a bag permits duplicates, it has no primary key
 *
 * @author Gavin King
 */
public non-sealed class Bag extends Collection {
	/**
	 * hbm.xml binding
	 */
	public Bag(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );
	}

	/**
	 * Annotation binding
	 */
	public Bag(Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver, PersistentClass owner, MetadataBuildingContext buildingContext) {
		super( customTypeBeanResolver, owner, buildingContext );
	}

	private Bag(Collection original) {
		super( original );
	}

	@Override
	public Bag copy() {
		return new Bag( this );
	}

	public CollectionType getDefaultCollectionType() {
		return new BagType( getRole(), getReferencedPropertyName() );
	}

	void createPrimaryKey() {
		//create an index on the key columns??
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
}
