/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import org.hibernate.annotations.common.reflection.XProperty;

/**
 * @author Steve Ebersole
 */
public interface AttributeConverterAutoApplyHandler {
	AttributeConverterDescriptor findAutoApplyConverterForAttribute(XProperty xProperty, MetadataBuildingContext context);
	AttributeConverterDescriptor findAutoApplyConverterForCollectionElement(XProperty xProperty, MetadataBuildingContext context);
	AttributeConverterDescriptor findAutoApplyConverterForMapKey(XProperty xProperty, MetadataBuildingContext context);
}
