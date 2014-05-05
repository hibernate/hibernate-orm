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
package org.hibernate.test.manytomany.batchload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import junit.framework.Assert;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Hibernate;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderImpl;
import org.hibernate.engine.jdbc.batch.internal.NonBatchingBatch;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.stat.CollectionStatistics;
import org.hibernate.testing.FailureExpectedWithNewUnifiedXsd;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Tests loading of many-to-many collection which should trigger
 * a batch load.
 *
 * @author Steve Ebersole
 */
@FailureExpectedWithNewUnifiedXsd(message = "New schema only defines batch-size at the class level, not collections.")
public class BatchedManyToManyTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "manytomany/batchload/UserGroupBatchLoad.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.BATCH_STRATEGY, TestingBatchBuilder.class.getName() );
	}

	public static class TestingBatchBuilder extends BatchBuilderImpl {
		@Override
		public Batch buildBatch(BatchKey key, JdbcCoordinator jdbcCoordinator) {
			return new TestingBatch( key, jdbcCoordinator );
		}
	}

	public static class TestingBatch extends NonBatchingBatch {
		public TestingBatch(BatchKey key, JdbcCoordinator jdbcCoordinator) {
			super( key, jdbcCoordinator );
		}
	}

	@Test
	public void testLoadingNonInverseSide() {
		prepareTestData();

		sessionFactory().getStatistics().clear();
		CollectionStatistics userGroupStats = sessionFactory().getStatistics()
				.getCollectionStatistics( User.class.getName() + ".groups" );
		CollectionStatistics groupUserStats = sessionFactory().getStatistics()
				.getCollectionStatistics( Group.class.getName() + ".users" );

		Interceptor testingInterceptor = new EmptyInterceptor() {
			@Override
            public String onPrepareStatement(String sql) {
				// ugh, this is the best way I could come up with to assert this.
				// unfortunately, this is highly dependent on the dialect and its
				// outer join fragment.  But at least this wil fail on the majority
				// of dialects...
				Assert.assertFalse(
						"batch load of many-to-many should use inner join",
						sql.toLowerCase().contains( "left outer join" )
				);
				return super.onPrepareStatement( sql );
			}
		};

		Session s = openSession( testingInterceptor );
		s.beginTransaction();
		List users = s.createQuery( "from User u" ).list();
		User user = ( User ) users.get( 0 );
		assertTrue( Hibernate.isInitialized( user ) );
		assertTrue( Hibernate.isInitialized( user.getGroups() ) );
		user = ( User ) users.get( 1 );
		assertTrue( Hibernate.isInitialized( user ) );
		assertTrue( Hibernate.isInitialized( user.getGroups() ) );
		assertEquals( 1, userGroupStats.getFetchCount() ); // should have been just one fetch (the batch fetch)
		assertEquals( 1, groupUserStats.getFetchCount() ); // should have been just one fetch (the batch fetch)
		s.getTransaction().commit();
		s.close();

	}

	protected void prepareTestData() {
		// set up the test data
		User me = new User( "steve" );
		User you = new User( "not steve" );
		Group developers = new Group( "developers" );
		Group translators = new Group( "translators" );
		Group contributors = new Group( "contributors" );
		me.getGroups().add( developers );
		developers.getUsers().add( me );
		you.getGroups().add( translators );
		translators.getUsers().add( you );
		you.getGroups().add( contributors );
		contributors.getUsers().add( you );
		Session s = openSession();
		s.beginTransaction();
		s.save( me );
		s.save( you );
		s.getTransaction().commit();
		s.close();
	}

	protected void cleanupTestData() {
		// clean up the test data
		Session s = openSession();
		s.beginTransaction();
		// User is the non-inverse side...
		List<User> users = s.createQuery( "from User" ).list();
		for ( User user : users ) {
			s.delete( user );
		}
		s.flush();
		s.createQuery( "delete Group" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}
}
