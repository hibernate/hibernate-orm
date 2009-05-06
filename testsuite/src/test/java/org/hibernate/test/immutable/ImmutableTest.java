//$Id: ImmutableTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.test.immutable;

import java.util.Iterator;

import junit.framework.Test;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class ImmutableTest extends FunctionalTestCase {

	public ImmutableTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "immutable/ContractVariation.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( ImmutableTest.class );
	}

	public void testImmutable() {
		Contract c = new Contract("gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		c.setCustomerName("foo bar");
		c.getVariations().add( new ContractVariation(3, c) );
		cv1 = (ContractVariation) c.getVariations().iterator().next();
		cv1.setText("blah blah");
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Integer(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Integer(0) );
		t.commit();
		s.close();
	}

	public void testImmutableParentEntityWithUpdate() {
		Contract c = new Contract("gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c.setCustomerName("foo bar");
		s.update( c );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Integer(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Integer(0) );
		t.commit();
		s.close();
	}

	public void testImmutableChildEntityWithUpdate() {
		Contract c = new Contract("gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		cv1 = (ContractVariation) c.getVariations().iterator().next();
		cv1.setText("blah blah");
		s.update( c );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Integer(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Integer(0) );
		t.commit();
		s.close();
	}

	public void testImmutableCollectionWithUpdate() {
		Contract c = new Contract("gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c.getVariations().add( new ContractVariation(3, c) );
		try {
			s.update( c );
			fail( "should have failed because reassociated object has a dirty collection");
		}
		catch ( HibernateException ex ) {
			// expected
		}
		finally {
			t.rollback();
			s.close();
		}

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Integer(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Integer(0) );
		t.commit();
		s.close();
	}

	public void testImmutableParentEntityWithMerge() {
		Contract c = new Contract("gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c.setCustomerName("foo bar");
		s.merge( c );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Integer(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Integer(0) );
		t.commit();
		s.close();
	}

	public void testImmutableChildEntityWithMerge() {
		Contract c = new Contract("gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		cv1 = (ContractVariation) c.getVariations().iterator().next();
		cv1.setText("blah blah");
		s.merge( c );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Integer(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Integer(0) );
		t.commit();
		s.close();
	}

	public void testImmutableCollectionWithMerge() {
		Contract c = new Contract("gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c.getVariations().add( new ContractVariation(3, c) );
		s.merge( c );
		try {
			t.commit();
			fail( "should have failed because an immutable collection was changed");
		}
		catch ( HibernateException ex ) {
			// expected
			t.rollback();
		}
		finally {
			s.close();
		}

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Integer(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Integer(0) );
		t.commit();
		s.close();
	}
}

