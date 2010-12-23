package org.hibernate.test.insertordering;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.sql.PreparedStatement;

import junit.framework.Test;

import org.hibernate.engine.jdbc.batch.internal.BatchBuilder;
import org.hibernate.engine.jdbc.batch.internal.BatchingBatch;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.spi.SQLExceptionHelper;
import org.hibernate.engine.jdbc.spi.SQLStatementLogger;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.Session;
import org.hibernate.jdbc.Expectation;

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
		cfg.setProperty( Environment.BATCH_STRATEGY, StatsBatchBuilder.class.getName() );
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
		StatsBatch.reset();
		s.getTransaction().commit();
		s.close();

		assertEquals( 3, StatsBatch.batchSizes.size() );

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

	public static class StatsBatch extends BatchingBatch {
		private static String batchSQL;
		private static List batchSizes = new ArrayList();
		private static int currentBatch = -1;

		public StatsBatch(Object key, SQLStatementLogger statementLogger, SQLExceptionHelper exceptionHelper, int jdbcBatchSize) {
			super( key, statementLogger, exceptionHelper, jdbcBatchSize );
		}

		static void reset() {
			batchSizes = new ArrayList();
			currentBatch = -1;
			batchSQL = null;
		}

		public void addBatchStatement(Object key, String sql, PreparedStatement ps) {
			if ( batchSQL == null || ! batchSQL.equals( sql ) ) {
				currentBatch++;
				batchSQL = sql;
				batchSizes.add( currentBatch, new Counter() );
				System.out.println( "--------------------------------------------------------" );
				System.out.println( "Preparing statement [" + sql + "]" );
			}
			super.addBatchStatement( key, sql, ps );
		}

		public void addToBatch(Object key, String sql, Expectation expectation) {
			Counter counter = ( Counter ) batchSizes.get( currentBatch );
			counter.count++;
			System.out.println( "Adding to batch [" + batchSQL + "]" );
			super.addToBatch( key, sql, expectation );
		}

		protected void doExecuteBatch() {
			System.out.println( "executing batch [" + batchSQL + "]" );
			System.out.println( "--------------------------------------------------------" );
			super.doExecuteBatch();
		}
	}

	public static class StatsBatchBuilder extends BatchBuilder {
		private int jdbcBatchSize;

		public void setJdbcBatchSize(int jdbcBatchSize) {
			this.jdbcBatchSize = jdbcBatchSize;
		}
		public Batch buildBatch(Object key, SQLStatementLogger statementLogger, SQLExceptionHelper exceptionHelper) {
			return new StatsBatch(key, statementLogger, exceptionHelper, jdbcBatchSize );
		}
	}
}
