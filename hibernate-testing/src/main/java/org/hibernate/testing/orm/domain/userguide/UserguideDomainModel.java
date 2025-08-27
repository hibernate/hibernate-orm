/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.userguide;


import org.hibernate.testing.orm.domain.AbstractDomainModelDescriptor;
import org.hibernate.testing.orm.domain.userguide.tooling.Customer;
import org.hibernate.testing.orm.domain.userguide.tooling.Item;
import org.hibernate.testing.orm.domain.userguide.tooling.Order;

/**
 * Model used mostly by tests referenced by the {@code documentation} project.
 *
 * @author Marco Belladelli
 */
public class UserguideDomainModel extends AbstractDomainModelDescriptor {
	public static final UserguideDomainModel INSTANCE = new UserguideDomainModel();

	public UserguideDomainModel() {
		super(
				Account.class,
				Call.class,
				CreditCardPayment.class,
				Image.class,
				Partner.class,
				Payment.class,
				Person.class,
				Phone.class,
				WireTransferPayment.class,
				Customer.class,
				Item.class,
				Order.class
		);
	}
}
