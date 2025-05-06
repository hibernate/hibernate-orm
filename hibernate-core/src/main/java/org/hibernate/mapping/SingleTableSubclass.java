/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.collections.JoinedList;

/**
 * A mapping model object that represents a subclass in a
 * {@linkplain jakarta.persistence.InheritanceType#SINGLE_TABLE single table}
 * inheritance hierarchy.
 *
 * @author Gavin King
 */
public final class SingleTableSubclass extends Subclass {

	public SingleTableSubclass(PersistentClass superclass, MetadataBuildingContext buildingContext) {
		super( superclass, buildingContext );
	}

	protected List<Property> getNonDuplicatedProperties() {
		return new JoinedList<>( getSuperclass().getUnjoinedProperties(), getUnjoinedProperties() );
	}

	public Object accept(PersistentClassVisitor mv) {
		return mv.accept( this );
	}

	public void validate(Metadata mapping) throws MappingException {
		if ( getDiscriminator() == null ) {
			throw new MappingException( "No discriminator defined by '" + getSuperclass().getEntityName()
					+ "' which is a root class in a 'SINGLE_TABLE' inheritance hierarchy"
			);
		}
		super.validate( mapping );
	}
}
