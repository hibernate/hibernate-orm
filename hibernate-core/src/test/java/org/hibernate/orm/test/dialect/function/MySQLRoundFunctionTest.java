/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.function;
import java.math.BigDecimal;

import org.junit.Test;

import org.hibernate.query.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
@author Strong Liu
 */
@RequiresDialect( MySQLDialect.class )
public class MySQLRoundFunctionTest extends BaseCoreFunctionalTestCase {

	@Override
	public String[] getMappings() {
		return new String[]{"dialect/function/Product.hbm.xml"};
	}

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Test
	public void testRoundFunction(){
		Product product = new Product();
		product.setLength( 100 );
		product.setPrice( new BigDecimal( 1.298 ) );
		Session s=openSession();
		Transaction tx=s.beginTransaction();
		s.persist( product );
		tx.commit();
		s.close();
		s=openSession();
		tx=s.beginTransaction();
		Query q=s.createQuery( "select round(p.price,1) from Product p" );
		Object o=q.uniqueResult();
		assertEquals( BigDecimal.class , o.getClass() );
		assertEquals( BigDecimal.valueOf( 1.3 ) , o );
		tx.commit();
		s.close();

	}

}
