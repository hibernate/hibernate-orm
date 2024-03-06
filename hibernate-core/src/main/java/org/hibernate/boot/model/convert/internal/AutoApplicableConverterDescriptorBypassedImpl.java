/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
	public ConverterDescriptor getAutoAppliedConverterDescriptorForAttribute(
			MemberDetails memberDetails,
			MetadataBuildingContext context) {
		return null;
	}

	@Override
	public ConverterDescriptor getAutoAppliedConverterDescriptorForCollectionElement(
			MemberDetails memberDetails,
			MetadataBuildingContext context) {
		return null;
	}

	@Override
	public ConverterDescriptor getAutoAppliedConverterDescriptorForMapKey(
			MemberDetails memberDetails,
			MetadataBuildingContext context) {
		return null;
	}
}
