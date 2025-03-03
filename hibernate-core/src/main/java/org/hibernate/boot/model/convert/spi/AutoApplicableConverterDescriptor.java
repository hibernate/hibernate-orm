/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	boolean isAutoApplicable();
	ConverterDescriptor getAutoAppliedConverterDescriptorForAttribute(MemberDetails memberDetails, MetadataBuildingContext context);
	ConverterDescriptor getAutoAppliedConverterDescriptorForCollectionElement(MemberDetails memberDetails, MetadataBuildingContext context);
	ConverterDescriptor getAutoAppliedConverterDescriptorForMapKey(MemberDetails memberDetails, MetadataBuildingContext context);
}
