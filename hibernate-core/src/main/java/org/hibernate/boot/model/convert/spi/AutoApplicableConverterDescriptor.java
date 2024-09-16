/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
	ConverterDescriptor getAutoAppliedConverterDescriptorForAttribute(MemberDetails memberDetails, MetadataBuildingContext context);
	ConverterDescriptor getAutoAppliedConverterDescriptorForCollectionElement(MemberDetails memberDetails, MetadataBuildingContext context);
	ConverterDescriptor getAutoAppliedConverterDescriptorForMapKey(MemberDetails memberDetails, MetadataBuildingContext context);
}
