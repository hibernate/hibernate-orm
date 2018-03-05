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
 * Contract for handling auto-apply checks for JPA AttributeConverters
 * 
 * @author Steve Ebersole
 */
public interface AutoApplicableConverterDescriptor {
	ConverterDescriptor getAutoAppliedConverterDescriptorForAttribute(XProperty xProperty, MetadataBuildingContext context);
	ConverterDescriptor getAutoAppliedConverterDescriptorForCollectionElement(XProperty xProperty, MetadataBuildingContext context);
	ConverterDescriptor getAutoAppliedConverterDescriptorForMapKey(XProperty xProperty, MetadataBuildingContext context);
}
