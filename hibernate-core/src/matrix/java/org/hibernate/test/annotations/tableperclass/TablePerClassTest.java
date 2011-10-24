/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.tableperclass;

import java.util.List;

import org.junit.Test;

import org.hibernate.JDBCException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class TablePerClassTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testUnionSubClass() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Machine computer = new Machine();
		computer.setWeight( new Double( 4 ) );
		Robot asimov = new Robot();
		asimov.setWeight( new Double( 120 ) );
		asimov.setName( "Asimov" );
		T800 terminator = new T800();
		terminator.setName( "Terminator" );
		terminator.setWeight( new Double( 300 ) );
		terminator.setTargetName( "Sarah Connor" );
		s.persist( computer );
		s.persist( asimov );
		s.persist( terminator );
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		Query q = s.createQuery( "from Machine m where m.weight >= :weight" );
		q.setDouble( "weight", new Double( 10 ) );
		List result = q.list();
		assertEquals( 2, result.size() );
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		tx.commit();
		s.close();
	}

	@Test
	public void testConstraintsOnSuperclassProperties() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Product product1 = new Product();
		product1.setId( 1l );
		product1.setManufacturerId( 1l );
		product1.setManufacturerPartNumber( "AAFR");
		s.persist( product1 );
		s.flush();
		Product product2 = new Product();
		product2.setId( 2l );
		product2.setManufacturerId( 1l );
		product2.setManufacturerPartNumber( "AAFR");
		s.persist( product2 );
		try {
			s.flush();
			fail("Database Exception not handled");
		}
		catch( JDBCException e ) {
			//success
		}
		tx.rollback();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Robot.class,
				T800.class,
				Machine.class,
				Component.class,
				Product.class
		};
	}
}
