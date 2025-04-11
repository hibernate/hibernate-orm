/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.convert.internal;

import org.hibernate.boot.model.convert.spi.AutoApplicableConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.spi.MemberDetails;

/**
 * An implementation of AutoApplicableConverterDescriptor that always reports
 * no auto-apply match
 *
 * @author Steve Ebersole
 */
public class AutoApplicableConverterDescriptorBypassedImpl implements AutoApplicableConverterDescriptor {
	/**
	 * Singleton access
	 */
	public static final AutoApplicableConverterDescriptorBypassedImpl INSTANCE = new AutoApplicableConverterDescriptorBypassedImpl();

	private AutoApplicableConverterDescriptorBypassedImpl() {
	}

	@Override
	public boolean isAutoApplicable() {
		return false;
	}

	@Override
	public ConverterDescriptor<?,?> getAutoAppliedConverterDescriptorForAttribute(
			MemberDetails memberDetails,
			MetadataBuildingContext context) {
		return null;
	}

	@Override
	public ConverterDescriptor<?,?> getAutoAppliedConverterDescriptorForCollectionElement(
			MemberDetails memberDetails,
			MetadataBuildingContext context) {
		return null;
	}

	@Override
	public ConverterDescriptor<?,?> getAutoAppliedConverterDescriptorForMapKey(
			MemberDetails memberDetails,
			MetadataBuildingContext context) {
		return null;
	}
}
