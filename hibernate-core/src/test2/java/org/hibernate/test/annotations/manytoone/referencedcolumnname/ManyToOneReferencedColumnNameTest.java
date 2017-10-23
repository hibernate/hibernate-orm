/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.manytoone.referencedcolumnname;

import java.math.BigDecimal;

import org.hibernate.Session;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class ManyToOneReferencedColumnNameTest extends BaseCoreFunctionalTestCase {
	@Test
	@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
	public void testReoverableExceptionInFkOrdering() throws Exception {
		//SF should not blow up
		Vendor v = new Vendor();
		Item i = new Item();
		ZItemCost ic = new ZItemCost();
		ic.setCost( new BigDecimal( 2 ) );
		ic.setItem( i );
		ic.setVendor( v );
		WarehouseItem wi = new WarehouseItem();
		wi.setDefaultCost( ic );
		wi.setItem( i );
		wi.setVendor( v );
		wi.setQtyInStock( new BigDecimal( 2 ) );
		Session s = openSession();
		s.getTransaction().begin();
		s.save( i );
		s.save( v );
		s.save( ic );
		s.save( wi );
		s.flush();
		s.getTransaction().rollback();
		s.close();
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setImplicitNamingStrategy( ImplicitNamingStrategyLegacyJpaImpl.INSTANCE );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Item.class,
				Vendor.class,
				WarehouseItem.class,
				ZItemCost.class
		};
	}
}
