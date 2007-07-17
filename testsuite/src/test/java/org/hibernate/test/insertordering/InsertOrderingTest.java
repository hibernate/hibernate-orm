package org.hibernate.test.insertordering;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.sql.SQLException;
import java.sql.PreparedStatement;

import junit.framework.Test;

import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.Session;
import org.hibernate.Interceptor;
import org.hibernate.HibernateException;
import org.hibernate.jdbc.BatchingBatcher;
import org.hibernate.jdbc.ConnectionManager;
import org.hibernate.jdbc.Expectation;
import org.hibernate.jdbc.BatcherFactory;
import org.hibernate.jdbc.Batcher;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class InsertOrderingTest extends FunctionalTestCase {
	public InsertOrderingTest(String string) {
		super( string );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( InsertOrderingTest.class );
	}

	public String[] getMappings() {
		return new String[] { "insertordering/Mapping.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.ORDER_INSERTS, "true" );
		cfg.setProperty( Environment.STATEMENT_BATCH_SIZE, "10" );
		cfg.setProperty( Environment.BATCH_STRATEGY, StatsBatcherFactory.class.getName() );
	}

	public void testBatchOrdering() {
		Session s = openSession();
		s.beginTransaction();
		int iterations = 12;
		for ( int i = 0; i < iterations; i++ ) {
			User user = new User( "user-" + i );
			Group group = new Group( "group-" + i );
			s.save( user );
			s.save( group );
			user.addMembership( group );
		}
		StatsBatcher.reset();
		s.getTransaction().commit();
		s.close();

		assertEquals( 6, StatsBatcher.batchSizes.size() );  // 2 batches of each insert statement

		s = openSession();
		s.beginTransaction();
		Iterator users = s.createQuery( "from User u left join fetch u.memberships m left join fetch m.group" ).list().iterator();
		while ( users.hasNext() ) {
			s.delete( users.next() );
		}
		s.getTransaction().commit();
		s.close();
	}

	public static class Counter {
		public int count = 0;
	}

	public static class StatsBatcher extends BatchingBatcher {
		private static String batchSQL;
		private static List batchSizes = new ArrayList();
		private static int currentBatch = -1;

		public StatsBatcher(ConnectionManager connectionManager, Interceptor interceptor) {
			super( connectionManager, interceptor );
		}

		static void reset() {
			batchSizes = new ArrayList();
			currentBatch = -1;
			batchSQL = null;
		}

		public PreparedStatement prepareBatchStatement(String sql) throws SQLException {
			PreparedStatement rtn = super.prepareBatchStatement( sql );
			if ( batchSQL == null || !batchSQL.equals( sql ) ) {
				currentBatch++;
				batchSQL = sql;
				batchSizes.add( currentBatch, new Counter() );
				System.out.println( "--------------------------------------------------------" );
				System.out.println( "Preparing statement [" + sql + "]" );
			}
			return rtn;
		}

		public void addToBatch(Expectation expectation) throws SQLException, HibernateException {
			Counter counter = ( Counter ) batchSizes.get( currentBatch );
			counter.count++;
			System.out.println( "Adding to batch [" + batchSQL + "]" );
			super.addToBatch( expectation );
		}

		protected void doExecuteBatch(PreparedStatement ps) throws SQLException, HibernateException {
			System.out.println( "executing batch [" + batchSQL + "]" );
			System.out.println( "--------------------------------------------------------" );
			batchSQL = null;
			super.doExecuteBatch( ps );
		}
	}

	public static class StatsBatcherFactory implements BatcherFactory {
		public Batcher createBatcher(ConnectionManager connectionManager, Interceptor interceptor) {
			return new StatsBatcher( connectionManager, interceptor );
		}
	}
}
