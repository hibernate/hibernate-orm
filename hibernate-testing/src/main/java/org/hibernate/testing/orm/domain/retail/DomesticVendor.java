/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.retail;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * @author Steve Ebersole
 */
@Entity
@DiscriminatorValue( "domestic" )
public class DomesticVendor extends Vendor {
	public DomesticVendor() {
	}

	public DomesticVendor(Integer id, String name, String billingEntity) {
		super( id, name, billingEntity );
	}

	public DomesticVendor(Integer id, String name, String billingEntity, String supplementalDetail) {
		super( id, name, billingEntity, supplementalDetail );
	}
}
