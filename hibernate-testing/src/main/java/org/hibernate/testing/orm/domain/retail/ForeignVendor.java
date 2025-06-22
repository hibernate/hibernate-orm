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
@DiscriminatorValue( "foreign" )
public class ForeignVendor extends Vendor {
	public ForeignVendor() {
	}

	public ForeignVendor(Integer id, String name, String billingEntity) {
		super( id, name, billingEntity );
	}

	public ForeignVendor(Integer id, String name, String billingEntity, String supplementalDetail) {
		super( id, name, billingEntity, supplementalDetail );
	}
}
