/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.binder.internal;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.AttributeAccessor;
import org.hibernate.binder.AttributeBinder;
import org.hibernate.binder.AttributeBindingContext;
import org.hibernate.property.access.spi.PropertyAccessStrategy;

/**
 * Configures the {@link PropertyAccessStrategy} for an attribute.
 *
 * @author Gavin King
 */
public class AttributeAccessorBinder implements AttributeBinder<AttributeAccessor> {
	@Override
	public void bind(AttributeAccessor accessor, AttributeBindingContext context) {
		final var type = accessor.strategy();
		if ( !PropertyAccessStrategy.class.equals(type) ) {
			context.getProperty().setPropertyAccessorName( type.getName() );
		}
		else {
			throw new AnnotationException("'@AttributeAccessor' annotation must specify a 'strategy'");
		}
	}
}
