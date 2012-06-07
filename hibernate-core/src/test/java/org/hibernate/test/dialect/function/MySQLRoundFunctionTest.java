/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.dialect.function;
import java.math.BigDecimal;

import org.junit.Test;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
  @author Strong Liu <stliu@redhat.com>
 */
@RequiresDialect( MySQLDialect.class )
public class MySQLRoundFunctionTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[]{"dialect/function/Product.hbm.xml"};
	}

	@Test
	public void testRoundFunction(){
		Product product = new Product();
		product.setLength( 100 );
		product.setPrice( new BigDecimal( 1.298 ) );
		Session s=openSession();
		Transaction tx=s.beginTransaction();
		s.save( product );
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
