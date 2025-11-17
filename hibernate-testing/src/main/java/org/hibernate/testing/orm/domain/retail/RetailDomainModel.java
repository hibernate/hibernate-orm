/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.retail;

import java.util.EnumSet;

import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.orm.domain.AbstractDomainModelDescriptor;
import org.hibernate.testing.orm.domain.MappingFeature;
import org.hibernate.testing.orm.domain.MonetaryAmountConverter;

import static org.hibernate.testing.orm.domain.MappingFeature.CONVERTER;
import static org.hibernate.testing.orm.domain.MappingFeature.EMBEDDABLE;
import static org.hibernate.testing.orm.domain.MappingFeature.JOINED_INHERIT;
import static org.hibernate.testing.orm.domain.MappingFeature.JOIN_COLUMN;
import static org.hibernate.testing.orm.domain.MappingFeature.MANY_ONE;
import static org.hibernate.testing.orm.domain.MappingFeature.SECONDARY_TABLE;

/**
 * @author Steve Ebersole
 */
public class RetailDomainModel extends AbstractDomainModelDescriptor {
	public static final RetailDomainModel INSTANCE = new RetailDomainModel();

	public RetailDomainModel() {
		super(
				MonetaryAmountConverter.class,
				SalesAssociate.class,
				Vendor.class,
				DomesticVendor.class,
				ForeignVendor.class,
				Product.class,
				Order.class,
				LineItem.class,
				Payment.class,
				CashPayment.class,
				CardPayment.class
		);
	}

	public static void applyRetailModel(MetadataSources sources) {
		INSTANCE.applyDomainModel( sources );
	}

	@Override
	public EnumSet<MappingFeature> getMappingFeaturesUsed() {
		return EnumSet.of(
				CONVERTER,
				EMBEDDABLE,
				MANY_ONE,
				JOIN_COLUMN,
				SECONDARY_TABLE,
				JOINED_INHERIT
		);
	}
}
