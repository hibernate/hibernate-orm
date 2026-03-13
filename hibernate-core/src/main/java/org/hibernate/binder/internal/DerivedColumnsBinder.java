/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.binder.internal;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.DerivedColumns;
import org.hibernate.binder.AttributeBinder;
import org.hibernate.binder.TypeBinder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

/**
 * Handles {@link DerivedColumns} annotations.
 */
public class DerivedColumnsBinder implements AttributeBinder<DerivedColumns>, TypeBinder<DerivedColumns> {

	@Override
	public void bind(DerivedColumns annotation, MetadataBuildingContext context, PersistentClass entity) {
		final var binder = new DerivedColumnBinder();
		for ( var derivedColumn : annotation.value() ) {
			binder.bind( derivedColumn, context, entity );
		}
	}

	@Override
	public void bind(DerivedColumns annotation, MetadataBuildingContext context, Component embeddable) {
		throw new AnnotationException( "Embeddable class '" + embeddable.getComponentClassName()
				+ "' was annotated '@DerivedColumn' (annotate its attributes instead)" );
	}

	@Override
	public void bind(DerivedColumns annotation, MetadataBuildingContext context, PersistentClass entity, Property property) {
		final var binder = new DerivedColumnBinder();
		for ( var derivedColumn : annotation.value() ) {
			binder.bind( derivedColumn, context, entity, property );
		}
	}
}
