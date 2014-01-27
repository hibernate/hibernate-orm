/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.manytoone.referencedcolumnname;

import java.math.BigDecimal;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Emmanuel Bernard
 */
@FailureExpectedWithNewMetamodel
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
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Item.class,
				Vendor.class,
				WarehouseItem.class,
				ZItemCost.class
		};
	}
}
