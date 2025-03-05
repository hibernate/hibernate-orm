/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.binder.internal;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.BatchSize;
import org.hibernate.binder.AttributeBinder;
import org.hibernate.binder.TypeBinder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;

/**
 * Binder for the {@link BatchSize} annotation.
 *
 * @since 6.5
 *
 * @author Gavin King
 */
public class BatchSizeBinder implements TypeBinder<BatchSize>, AttributeBinder<BatchSize> {
	@Override
	public void bind(BatchSize batchSize, MetadataBuildingContext context, PersistentClass persistentClass) {
		persistentClass.setBatchSize( batchSize.size() );
	}

	@Override
	public void bind(BatchSize batchSize, MetadataBuildingContext context, Component embeddableClass) {
		throw new AnnotationException("Class '" + embeddableClass.getComponentClassName()
				+ "' is an '@Embeddable' type and may not be annotated '@BatchSize'");
	}

	@Override
	public void bind(BatchSize batchSize, MetadataBuildingContext context, PersistentClass persistentClass, Property property) {
		final Value value = property.getValue();
		if ( value instanceof Collection collection ) {
			collection.setBatchSize( batchSize.size() );
		}
		else {
			throw new AnnotationException("Property '" + property.getName() + "' may not be annotated '@BatchSize'");
		}
	}
}
