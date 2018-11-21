/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.support.domains.retail;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.support.MonetaryAmountConverter;
import org.hibernate.orm.test.support.domains.DomainModel;

/**
 * @author Steve Ebersole
 */
public class RetailDomainModel implements DomainModel {
	public static final RetailDomainModel INSTANCE = new RetailDomainModel();

	private static final Class[] CLASSES = new Class[] {
			MonetaryAmountConverter.class,
			SalesAssociate.class,
			Vendor.class,
			Product.class,
			Order.class,
			LineItem.class,
			Payment.class,
			CashPayment.class,
			CardPayment.class
	};

	public static void applyRetailModel(MetadataSources sources) {
		for ( Class domainClass : CLASSES ) {
			sources.addAnnotatedClass( domainClass );
		}
	}

	private RetailDomainModel() {
	}

	@Override
	public void applyDomainModel(MetadataSources sources) {
		applyRetailModel( sources );
	}
}
