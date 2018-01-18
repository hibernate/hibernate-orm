/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.convert.spi;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * @author Steve Ebersole
 */
public interface ConverterAutoApplyHandler {
	ConverterDescriptor findAutoApplyConverterForAttribute(XProperty xProperty, MetadataBuildingContext context);
	ConverterDescriptor findAutoApplyConverterForCollectionElement(XProperty xProperty, MetadataBuildingContext context);
	ConverterDescriptor findAutoApplyConverterForMapKey(XProperty xProperty, MetadataBuildingContext context);
}
