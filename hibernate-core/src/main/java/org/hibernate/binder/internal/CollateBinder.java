/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.binder.internal;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.Collate;
import org.hibernate.binder.AttributeBinder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;

/**
 * Handles {@link Collate} annotations.
 *
 * @author Gavin King
 */
public class CollateBinder implements AttributeBinder<Collate> {
	@Override
	public void bind(Collate collate, MetadataBuildingContext context, PersistentClass entity, Property property) {
		Value value = property.getValue();
		if ( value instanceof OneToMany ) {
			throw new AnnotationException( "One to many association '" + property.getName()
					+ "' was annotated '@Collate'");
		}
		else if ( value instanceof Collection ) {
			throw new AnnotationException( "Collection '" + property.getName()
					+ "' was annotated '@Collate'");

		}
		else {
			for ( Column column : value.getColumns() ) {
				column.setCollation( collate.value() );
			}
		}
	}
}
