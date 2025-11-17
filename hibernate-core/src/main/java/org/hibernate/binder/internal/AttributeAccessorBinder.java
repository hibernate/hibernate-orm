/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.binder.internal;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.AttributeAccessor;
import org.hibernate.binder.AttributeBinder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.property.access.spi.PropertyAccessStrategy;

/**
 * Configures the {@link PropertyAccessStrategy} for an attribute.
 *
 * @author Gavin King
 */
public class AttributeAccessorBinder implements AttributeBinder<AttributeAccessor> {
	@Override
	public void bind(
			AttributeAccessor accessor,
			MetadataBuildingContext buildingContext,
			PersistentClass persistentClass,
			Property property) {
		final String value = accessor.value();
		final Class<?> type = accessor.strategy();
		if ( !value.isEmpty() ) {
			property.setPropertyAccessorName( value );
		}
		else if ( !PropertyAccessStrategy.class.equals(type) ) {
			property.setPropertyAccessorName( type.getName() );
		}
		else {
			throw new AnnotationException("'@AttributeAccessor' annotation must specify a 'strategy'");
		}
	}
}
