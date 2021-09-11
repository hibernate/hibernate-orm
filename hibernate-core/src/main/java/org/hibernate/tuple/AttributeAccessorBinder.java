/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.AttributeAccessor;
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
		String value = accessor.value();
		Class<?> type = accessor.strategy();
		if ( !value.isEmpty() ) {
			property.setPropertyAccessorName( value );
		}
		else if ( !PropertyAccessStrategy.class.equals(type) ) {
			property.setPropertyAccessorName( type.getName() );
		}
		else {
			throw new AnnotationException("@AttributeAccessor must specify a PropertyAccessStrategy type");
		}

	}
}
