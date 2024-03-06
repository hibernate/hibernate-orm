/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.convert.spi;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.spi.MemberDetails;

/**
 * Contract for handling auto-apply checks for JPA AttributeConverters
 * 
 * @author Steve Ebersole
 */
public interface AutoApplicableConverterDescriptor {
	ConverterDescriptor getAutoAppliedConverterDescriptorForAttribute(MemberDetails memberDetails, MetadataBuildingContext context);
	ConverterDescriptor getAutoAppliedConverterDescriptorForCollectionElement(MemberDetails memberDetails, MetadataBuildingContext context);
	ConverterDescriptor getAutoAppliedConverterDescriptorForMapKey(MemberDetails memberDetails, MetadataBuildingContext context);
}
