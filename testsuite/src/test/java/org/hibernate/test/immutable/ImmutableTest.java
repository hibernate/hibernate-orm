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

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
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

	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true");
		cfg.setProperty( Environment.STATEMENT_BATCH_SIZE, "0" );
	}	

	public String[] getMappings() {
		return new String[] { "immutable/ContractVariation.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( ImmutableTest.class );
	}

	public void testPersistImmutable() {
		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");

		clearCounts();

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		// c, cv1, and cv2 were added to s by s.persist(c) (not hibernate), so they are modifiable
		assertFalse( s.isReadOnly( c ) );
		assertFalse( s.isReadOnly( cv1 ) );
		assertFalse( s.isReadOnly( cv2 ) );
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		// c was loaded into s by hibernate, so it should be read-only
		assertTrue( s.isReadOnly( c ) );
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		// cv1 and cv2 were loaded into s by hibernate, so they should be read-only
		assertTrue( s.isReadOnly( cv1 ) );
		assertTrue( s.isReadOnly( cv2 ) );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	public void testPersistUpdateImmutableInSameTransaction() {
		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");

		clearCounts();

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		// c, cv1, and cv2 were added to s by s.persist(c) (not hibernate), so they are modifiable
		assertFalse( s.isReadOnly( c ) );
		assertFalse( s.isReadOnly( cv1 ) );
		assertFalse( s.isReadOnly( cv2 ) );
		c.setCustomerName( "gail" );
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		// c was loaded into s by hibernate, so it should be read-only
		assertTrue( s.isReadOnly( c ) );
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		// cv1 and cv2 were loaded into s by hibernate, so they should be read-only
		assertTrue( s.isReadOnly( cv1 ) );
		assertTrue( s.isReadOnly( cv2 ) );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	public void testSaveImmutable() {
		clearCounts();

		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.save(c);
		// c, cv1, and cv2 were added to s by s.persist(c) (not hibernate), so they are modifiable
		assertFalse( s.isReadOnly( c ) );
		assertFalse( s.isReadOnly( cv1 ) );
		assertFalse( s.isReadOnly( cv2 ) );
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		// c was loaded into s by hibernate, so it should be read-only
		assertTrue( s.isReadOnly( c ) );
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		// cv1 and cv2 were loaded into s by hibernate, so they should be read-only
		assertTrue( s.isReadOnly( cv1 ) );
		assertTrue( s.isReadOnly( cv2 ) );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	public void testSaveOrUpdateImmutable() {
		clearCounts();

		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.saveOrUpdate(c);
		// c, cv1, and cv2 were added to s by s.persist(c) (not hibernate), so they are modifiable
		assertFalse( s.isReadOnly( c ) );
		assertFalse( s.isReadOnly( cv1 ) );
		assertFalse( s.isReadOnly( cv2 ) );
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		// c was loaded into s by hibernate, so it should be read-only
		assertTrue( s.isReadOnly( c ) );
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		// cv1 and cv2 were loaded into s by hibernate, so they should be read-only
		assertTrue( s.isReadOnly( cv1 ) );
		assertTrue( s.isReadOnly( cv2 ) );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	public void testImmutable() {
		clearCounts();

		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		// c, cv1, and cv2 were added to s by s.persist(c) (not hibernate), so they are modifiable
		assertFalse( s.isReadOnly( c ) );
		assertFalse( s.isReadOnly( cv1 ) );
		assertFalse( s.isReadOnly( cv2 ) );
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		// c was loaded into s, so it should be read-only
		assertTrue( s.isReadOnly( c ) );
		c.setCustomerName("foo bar");
		c.getVariations().add( new ContractVariation(3, c) );
		cv1 = (ContractVariation) c.getVariations().iterator().next();
		cv1.setText("blah blah");
		// cv1 and cv2 were loaded into s by hibernate, so they should be read-only
		assertTrue( s.isReadOnly( cv1 ) );
		assertFalse( s.contains( cv2 ) );
		t.commit();
		assertTrue( s.isReadOnly( c ) );
		assertTrue( s.isReadOnly( cv1 ) );
		assertFalse( s.contains( cv2 ) );
		s.close();

		assertInsertCount( 0 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		// c was loaded into s by hibernate, so it should be read-only
		assertTrue( s.isReadOnly( c ) );
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		// cv1 and cv2 were loaded into s by hibernate, so they should be read-only
		assertTrue( s.isReadOnly( cv1 ) );
		assertTrue( s.isReadOnly( cv2 ) );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	public void testPersistAndUpdateImmutable() {
		clearCounts();

		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		// c, cv1, and cv2 were added to s by s.persist(c) (not hibernate), so they are modifiable
		assertFalse( s.isReadOnly( c ) );
		assertFalse( s.isReadOnly( cv1 ) );
		assertFalse( s.isReadOnly( cv2 ) );
		c.setCustomerName( "Sherman" );
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		// c was loaded into s, so it should be read-only
		assertTrue( s.isReadOnly( c ) );
		c.setCustomerName("foo bar");
		c.getVariations().add( new ContractVariation(3, c) );
		cv1 = (ContractVariation) c.getVariations().iterator().next();
		cv1.setText("blah blah");
		// cv1 and cv2 were loaded into s by hibernate, so they should be read-only
		assertTrue( s.isReadOnly( cv1 ) );
		assertFalse( s.contains( cv2 ) );
		t.commit();
		assertTrue( s.isReadOnly( c ) );
		assertTrue( s.isReadOnly( cv1 ) );
		assertFalse( s.contains( cv2 ) );
		s.close();

		assertInsertCount( 0 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		// c was loaded into s by hibernate, so it should be read-only
		assertTrue( s.isReadOnly( c ) );
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		// cv1 and cv2 were loaded into s by hibernate, so they should be read-only
		assertTrue( s.isReadOnly( cv1 ) );
		assertTrue( s.isReadOnly( cv2 ) );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	public void testUpdateAndDeleteManagedImmutable() {
		clearCounts();

		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		// c was loaded into s by hibernate, so it should be read-only
		assertTrue( s.isReadOnly( c ) );
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		// cv1 and cv2 were loaded into s by hibernate, so they should be read-only
		assertTrue( s.isReadOnly( cv1 ) );
		assertTrue( s.isReadOnly( cv2 ) );
		c.setCustomerName( "Sherman" );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	public void testGetAndDeleteManagedImmutable() {
		clearCounts();

		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.get( Contract.class, c.getId() );
		// c was loaded into s by hibernate, so it should be read-only
		assertTrue( s.isReadOnly( c ) );
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		// cv1 and cv2 were loaded into s by hibernate, so they should be read-only
		assertTrue( s.isReadOnly( cv1 ) );
		assertTrue( s.isReadOnly( cv2 ) );
		c.setCustomerName( "Sherman" );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	public void testDeleteDetachedImmutable() {
		clearCounts();

		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		s.delete( c );
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertNull( c );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	public void testDeleteDetachedModifiedImmutable() {
		clearCounts();

		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c.setCustomerName( "sherman" );
		s.delete( c );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );		
	}


	public void testImmutableParentEntityWithUpdate() {
		clearCounts();

		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c.setCustomerName("foo bar");
		s.update( c );
		// c was not loaded into s by hibernate, so it should be modifiable
		assertFalse( s.isReadOnly( c ) );
		assertFalse( s.contains( cv1 ) );
		assertFalse( s.contains( cv2 ) );
		t.commit();
		// c, cv1, and cv2 were not loaded into s by hibernate, so they are modifiable
		assertFalse( s.isReadOnly( c ) );
		assertFalse( s.isReadOnly( cv1 ) );
		assertFalse( s.isReadOnly( cv2 ) );		
		s.close();

		assertUpdateCount( 0 );

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
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	public void testImmutableChildEntityWithUpdate() {
		clearCounts();

		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		cv1 = (ContractVariation) c.getVariations().iterator().next();
		cv1.setText("blah blah");
		s.update( c );
		// c was not loaded into s by hibernate, so it should be modifiable
		assertFalse( s.isReadOnly( c ) );
		assertFalse( s.contains( cv1 ) );
		assertFalse( s.contains( cv2 ) );
		t.commit();
		assertFalse( s.isReadOnly( c ) );
		assertFalse( s.isReadOnly( cv1 ) );
		assertFalse( s.isReadOnly( cv2 ) );
		s.close();

		assertUpdateCount( 0 );

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
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	public void testImmutableCollectionWithUpdate() {
		clearCounts();

		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );

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

		assertUpdateCount( 0 );

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
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	public void testUnmodifiedImmutableParentEntityWithMerge() {
		clearCounts();

		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = ( Contract ) s.merge( c );
		// c was loaded into s by hibernate in the merge process, so it is read-only
		assertTrue( s.isReadOnly( c ) );
		assertTrue( Hibernate.isInitialized( c.getVariations() ) );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		cv2 = (ContractVariation) it.next();
		// cv1 and cv2 were loaded into s by hibernate in the merge process, so they are read-only
		assertTrue( s.isReadOnly( cv1 ) );
		assertTrue( s.isReadOnly( cv2 ) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	public void testImmutableParentEntityWithMerge() {
		clearCounts();

		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c.setCustomerName("foo bar");
		c = ( Contract ) s.merge( c );
		// c was loaded into s by hibernate in the merge process, so it is read-only
		assertTrue( s.isReadOnly( c ) );
		assertTrue( Hibernate.isInitialized( c.getVariations() ) );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		cv2 = (ContractVariation) it.next();
		// cv1 and cv2 were loaded into s by hibernate in the merge process, so they are read-only
		assertTrue( s.isReadOnly( c ) );
		assertTrue( s.isReadOnly( c ) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );

	}

	public void testImmutableChildEntityWithMerge() {
		clearCounts();

		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		cv1 = (ContractVariation) c.getVariations().iterator().next();
		cv1.setText("blah blah");
		c = ( Contract ) s.merge( c );
		// c was loaded into s by hibernate in the merge process, so it is read-only
		assertTrue( s.isReadOnly( c ) );
		assertTrue( Hibernate.isInitialized( c.getVariations() ) );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		cv2 = (ContractVariation) it.next();
		// cv1 and cv2 were loaded into s by hibernate in the merge process, so they are read-only
		assertTrue( s.isReadOnly( c ) );
		assertTrue( s.isReadOnly( c ) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	public void testImmutableCollectionWithMerge() {
		clearCounts();

		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );

		clearCounts();

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
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );		
	}

	protected void clearCounts() {
		getSessions().getStatistics().clear();
	}

	protected void assertInsertCount(int expected) {
		int inserts = ( int ) getSessions().getStatistics().getEntityInsertCount();
		assertEquals( "unexpected insert count", expected, inserts );
	}

	protected void assertUpdateCount(int expected) {
		int updates = ( int ) getSessions().getStatistics().getEntityUpdateCount();
		assertEquals( "unexpected update counts", expected, updates );
	}

	protected void assertDeleteCount(int expected) {
		int deletes = ( int ) getSessions().getStatistics().getEntityDeleteCount();
		assertEquals( "unexpected delete counts", expected, deletes );
	}
}

