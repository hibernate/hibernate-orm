/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import javax.persistence.AttributeConverter;

import org.hibernate.annotations.common.reflection.XProperty;

/**
 * Internal descriptor for an AttributeConverter implementation.
 *
 * @author Steve Ebersole
 */
public interface AttributeConverterDescriptor {
	AttributeConverter getAttributeConverter();
	Class<?> getDomainType();
	Class<?> getJdbcType();

	boolean shouldAutoApplyToAttribute(XProperty xProperty, MetadataBuildingContext context);
	boolean shouldAutoApplyToCollectionElement(XProperty xProperty, MetadataBuildingContext context);
	boolean shouldAutoApplyToMapKey(XProperty xProperty, MetadataBuildingContext context);

}
