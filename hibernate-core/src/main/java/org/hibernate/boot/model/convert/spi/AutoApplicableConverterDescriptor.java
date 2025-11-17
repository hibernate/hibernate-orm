/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.convert.spi;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.spi.MemberDetails;

/**
 * Contract for handling {@linkplain jakarta.persistence.Converter#autoApply auto-apply}
 * checks for JPA {@linkplain jakarta.persistence.AttributeConverter converters}.
 *
 * @author Steve Ebersole
 *
 * @see jakarta.persistence.Converter#autoApply
 */
public interface AutoApplicableConverterDescriptor {
	boolean isAutoApplicable();
	ConverterDescriptor<?,?> getAutoAppliedConverterDescriptorForAttribute(MemberDetails memberDetails, MetadataBuildingContext context);
	ConverterDescriptor<?,?> getAutoAppliedConverterDescriptorForCollectionElement(MemberDetails memberDetails, MetadataBuildingContext context);
	ConverterDescriptor<?,?> getAutoAppliedConverterDescriptorForMapKey(MemberDetails memberDetails, MetadataBuildingContext context);
}
