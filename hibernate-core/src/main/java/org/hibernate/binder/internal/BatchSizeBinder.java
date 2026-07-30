/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.binder.internal;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.BatchSize;
import org.hibernate.binder.AttributeBinder;
import org.hibernate.binder.AttributeBindingContext;
import org.hibernate.binder.EntityBindingContext;
import org.hibernate.binder.TypeBinder;
import org.hibernate.mapping.Collection;
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
	public void bind(BatchSize batchSize, EntityBindingContext context) {
		context.getPersistentClass().setBatchSize( batchSize.size() );
	}

	@Override
	public void bind(BatchSize batchSize, AttributeBindingContext context) {
		final Value value = context.getProperty().getValue();
		if ( value instanceof Collection collection ) {
			collection.setBatchSize( batchSize.size() );
		}
		else {
			throw new AnnotationException(
					"Property '" + context.getProperty().getName() + "' may not be annotated '@BatchSize'"
			);
		}
	}
}
