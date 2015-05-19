/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.immutable;
import java.util.Iterator;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Projections;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.TextType;
import org.hibernate.type.descriptor.sql.ClobTypeDescriptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Gavin King
 */
public class ImmutableTest extends BaseCoreFunctionalTestCase {
	private static class TextAsMaterializedClobType extends AbstractSingleColumnStandardBasicType<String> {
		public final static TextAsMaterializedClobType INSTANCE = new TextAsMaterializedClobType();
		public TextAsMaterializedClobType() {
			super(  ClobTypeDescriptor.DEFAULT, TextType.INSTANCE.getJavaTypeDescriptor() );
		}
		public String getName() {
			return TextType.INSTANCE.getName();
		}
	}

	@Override
	public void configure(Configuration cfg) {
		if ( Oracle8iDialect.class.isInstance( getDialect() ) ) {
			cfg.registerTypeOverride( TextAsMaterializedClobType.INSTANCE );
		}
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true");
		cfg.setProperty( Environment.STATEMENT_BATCH_SIZE, "0" );
	}	

	@Override
	public String[] getMappings() {
		return new String[] { "immutable/ContractVariation.hbm.xml" };
	}

	@Test
	public void testChangeImmutableEntityProxyToModifiable() {
		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");

		clearCounts();

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		assertTrue( s.isReadOnly( c ) );
		assertTrue( s.isReadOnly( cv1 ) );
		assertTrue( s.isReadOnly( cv2 ) );
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertTrue( s.isReadOnly( c ) );
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		assertTrue( s.isReadOnly( cv1 ) );
		assertTrue( s.isReadOnly( cv2 ) );

		try {
			assertTrue( c instanceof HibernateProxy );
			s.setReadOnly( c, false );
		}
		catch (IllegalStateException ex) {
			// expected
		}
		finally {
			t.rollback();
			s.close();
		}

		s = openSession();
		t = s.beginTransaction();
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testChangeImmutableEntityToModifiable() {
		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");

		clearCounts();

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		assertTrue( s.isReadOnly( c ) );
		assertTrue( s.isReadOnly( cv1 ) );
		assertTrue( s.isReadOnly( cv2 ) );
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertTrue( s.isReadOnly( c ) );
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		assertTrue( s.isReadOnly( cv1 ) );
		assertTrue( s.isReadOnly( cv2 ) );

		try {
			assertTrue( c instanceof HibernateProxy );
			s.setReadOnly( ( ( HibernateProxy ) c ).getHibernateLazyInitializer().getImplementation(), false );
		}
		catch (IllegalStateException ex) {
			// expected
		}
		finally {
			t.rollback();
			s.close();
		}

		s = openSession();
		t = s.beginTransaction();
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
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
		assertTrue( s.isReadOnly( c ) );
		assertTrue( s.isReadOnly( cv1 ) );
		assertTrue( s.isReadOnly( cv2 ) );
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertTrue( s.isReadOnly( c ) );
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
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

	@Test
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
		assertTrue( s.isReadOnly( c ) );
		assertTrue( s.isReadOnly( cv1 ) );
		assertTrue( s.isReadOnly( cv2 ) );
		c.setCustomerName( "gail" );
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertTrue( s.isReadOnly( c ) );
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
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

	@Test
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
		assertTrue( s.isReadOnly( c ) );
		assertTrue( s.isReadOnly( cv1 ) );
		assertTrue( s.isReadOnly( cv2 ) );
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertTrue( s.isReadOnly( c ) );
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
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

	@Test
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
		assertTrue( s.isReadOnly( c ) );
		assertTrue( s.isReadOnly( cv1 ) );
		assertTrue( s.isReadOnly( cv2 ) );
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertTrue( s.isReadOnly( c ) );
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
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

	@Test
	public void testRefreshImmutable() {
		clearCounts();

		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.saveOrUpdate( c );
		assertTrue( s.isReadOnly( c ) );
		assertTrue( s.isReadOnly( cv1 ) );
		assertTrue( s.isReadOnly( cv2 ) );
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		// refresh detached
		s.refresh( c );
		assertTrue( s.isReadOnly( c ) );
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		assertTrue( s.isReadOnly( cv1 ) );
		assertTrue( s.isReadOnly( cv2 ) );
		t.commit();
		s.close();

		assertInsertCount( 0 );
		assertUpdateCount( 0 );
		clearCounts();

		c.setCustomerName( "joe" );

		s = openSession();
		t = s.beginTransaction();
		// refresh updated detached
		s.refresh( c );
		assertTrue( s.isReadOnly( c ) );
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		assertTrue( s.isReadOnly( cv1 ) );
		assertTrue( s.isReadOnly( cv2 ) );
		t.commit();
		s.close();

		assertInsertCount( 0 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
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
		assertTrue( s.isReadOnly( c ) );
		assertTrue( s.isReadOnly( cv1 ) );
		assertTrue( s.isReadOnly( cv2 ) );
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertTrue( s.isReadOnly( c ) );
		c.setCustomerName("foo bar");
		cv1 = (ContractVariation) c.getVariations().iterator().next();
		cv1.setText("blah blah");
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
		assertTrue( s.isReadOnly( c ) );
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
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

	@Test
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
		assertTrue( s.isReadOnly( c ) );
		assertTrue( s.isReadOnly( cv1 ) );
		assertTrue( s.isReadOnly( cv2 ) );
		c.setCustomerName( "Sherman" );
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertTrue( s.isReadOnly( c ) );
		c.setCustomerName("foo bar");
		cv1 = (ContractVariation) c.getVariations().iterator().next();
		cv1.setText("blah blah");
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
		assertTrue( s.isReadOnly( c ) );
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
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

	@Test
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
		assertTrue( s.isReadOnly( c ) );
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
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

	@Test
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
		assertTrue( s.isReadOnly( c ) );
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
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

	@Test
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

	@Test
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


	@Test
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
		assertTrue( s.isReadOnly( c ) );
		for ( Iterator it = c.getVariations().iterator(); it.hasNext(); ) {
			assertTrue( s.contains( it.next() ) );
		}
		t.commit();
		assertTrue( s.isReadOnly( c ) );
		for ( Iterator it = c.getVariations().iterator(); it.hasNext(); ) {
			ContractVariation cv = ( ContractVariation ) it.next();
			assertTrue( s.contains( cv ) );
			assertTrue( s.isReadOnly( cv ) );
		}
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

	@Test
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
		assertTrue( s.isReadOnly( c ) );
		assertTrue( s.contains( cv1 ) );
		assertTrue( s.contains( cv2 ) );
		t.commit();
		assertTrue( s.isReadOnly( c ) );
		assertTrue( s.isReadOnly( cv1 ) );
		assertTrue( s.isReadOnly( cv2 ) );
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

	@Test
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
		s.update( c );
		try {
			t.commit();
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

	@Test
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
		assertTrue( s.isReadOnly( c ) );
		assertTrue( Hibernate.isInitialized( c.getVariations() ) );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		cv2 = (ContractVariation) it.next();
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

	@Test
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
		assertTrue( s.isReadOnly( c ) );
		assertTrue( Hibernate.isInitialized( c.getVariations() ) );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		cv2 = (ContractVariation) it.next();
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

	@Test
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
		assertTrue( s.isReadOnly( c ) );
		assertTrue( Hibernate.isInitialized( c.getVariations() ) );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		cv2 = (ContractVariation) it.next();
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

	@Test
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

	@Test
	public void testNewEntityViaImmutableEntityWithImmutableCollectionUsingSaveOrUpdate() {
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
		cv1.getInfos().add( new Info( "cv1 info" ) );
		s.saveOrUpdate( c );
		t.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( 0 );

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		assertEquals( 1, cv1.getInfos().size() );
		assertEquals( "cv1 info", ( ( Info ) cv1.getInfos().iterator().next() ).getText() );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 4 );
	}

	@Test
	public void testNewEntityViaImmutableEntityWithImmutableCollectionUsingMerge() {
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
		cv1.getInfos().add( new Info( "cv1 info" ) );
		s.merge( c );
		t.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( 0 );

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		assertEquals( 1, cv1.getInfos().size() );
		assertEquals( "cv1 info", ( ( Info ) cv1.getInfos().iterator().next() ).getText() );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 4 );
	}

	@Test
	public void testUpdatedEntityViaImmutableEntityWithImmutableCollectionUsingSaveOrUpdate() {
		clearCounts();

		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		Info cv1Info = new Info( "cv1 info" );
		cv1.getInfos().add( cv1Info );
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		assertInsertCount( 4 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		cv1Info.setText( "new cv1 info" );
		s.saveOrUpdate( c );
		t.commit();
		s.close();

		assertInsertCount( 0 );
		assertUpdateCount( 1 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		assertEquals( 1, cv1.getInfos().size() );
		assertEquals( "new cv1 info", ( ( Info ) cv1.getInfos().iterator().next() ).getText() );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 4 );
	}

	@Test
	public void testUpdatedEntityViaImmutableEntityWithImmutableCollectionUsingMerge() {
		clearCounts();

		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		Info cv1Info = new Info( "cv1 info" );
		cv1.getInfos().add( cv1Info );
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		assertInsertCount( 4 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		cv1Info.setText( "new cv1 info" );
		s.merge( c );
		t.commit();
		s.close();

		assertInsertCount( 0 );
		assertUpdateCount( 1 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		Iterator it = c.getVariations().iterator();
		cv1 = (ContractVariation) it.next();
		assertEquals( cv1.getText(), "expensive" );
		assertEquals( 1, cv1.getInfos().size() );
		assertEquals( "new cv1 info", ( ( Info ) cv1.getInfos().iterator().next() ).getText() );
		cv2 = (ContractVariation) it.next();
		assertEquals( cv2.getText(), "more expensive" );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 4 );
	}

	@Test
	public void testImmutableEntityAddImmutableToInverseMutableCollection() {
		clearCounts();

		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		Party party = new Party( "a party" );
		s.persist( party );
		t.commit();
		s.close();

		assertInsertCount( 4 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c.addParty( new Party( "a new party" ) );
		s.update( c );
		t.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c.addParty( party );
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
		//assertEquals( 2, c.getParties().size() );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 4 );
	}
	
	@Test
	public void testImmutableEntityRemoveImmutableFromInverseMutableCollection() {
		clearCounts();

		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Party party = new Party( "party1" );
		c.addParty( party );
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		assertInsertCount( 4 );
		assertUpdateCount( 0 );
		clearCounts();

		party = ( Party ) c.getParties().iterator().next();
		c.removeParty( party );

		s = openSession();
		t = s.beginTransaction();
		s.update( c );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		clearCounts();

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
		//assertEquals( 0, c.getParties().size() );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 4 );
	}

	@Test
	public void testImmutableEntityRemoveImmutableFromInverseMutableCollectionByDelete() {
		clearCounts();

		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Party party = new Party( "party1" );
		c.addParty( party );
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		assertInsertCount( 4 );
		assertUpdateCount( 0 );
		clearCounts();

		party = ( Party ) c.getParties().iterator().next();

		s = openSession();
		t = s.beginTransaction();
		s.delete( party );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 1 );
		clearCounts();

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
		assertEquals( 0, c.getParties().size() );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testImmutableEntityRemoveImmutableFromInverseMutableCollectionByDeref() {
		clearCounts();

		Contract c = new Contract( null, "gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Party party = new Party( "party1" );
		c.addParty( party );
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		assertInsertCount( 4 );
		assertUpdateCount( 0 );
		clearCounts();

		party = ( Party ) c.getParties().iterator().next();
		party.setContract( null );

		s = openSession();
		t = s.beginTransaction();
		s.update( party );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		party = ( Party ) s.get( Party.class, party.getId() );
		assertNotNull( party.getContract() );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		clearCounts();

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
		assertEquals( 1, c.getParties().size() );
	    party = ( Party ) c.getParties().iterator().next();
		assertEquals( "party1", party.getName() );
		assertSame( c, party.getContract() );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 4 );
	}

	protected void clearCounts() {
		sessionFactory().getStatistics().clear();
	}

	protected void assertInsertCount(int expected) {
		int inserts = ( int ) sessionFactory().getStatistics().getEntityInsertCount();
		assertEquals( "unexpected insert count", expected, inserts );
	}

	protected void assertUpdateCount(int expected) {
		int updates = ( int ) sessionFactory().getStatistics().getEntityUpdateCount();
		assertEquals( "unexpected update counts", expected, updates );
	}

	protected void assertDeleteCount(int expected) {
		int deletes = ( int ) sessionFactory().getStatistics().getEntityDeleteCount();
		assertEquals( "unexpected delete counts", expected, deletes );
	}
}

