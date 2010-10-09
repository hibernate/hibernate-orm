/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
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
 */
package org.hibernate.test.manytomany.batchload;

import java.util.List;

import junit.framework.Test;
import junit.framework.Assert;

import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.Session;
import org.hibernate.Hibernate;
import org.hibernate.Interceptor;
import org.hibernate.EmptyInterceptor;
import org.hibernate.jdbc.BatcherFactory;
import org.hibernate.jdbc.NonBatchingBatcher;
import org.hibernate.jdbc.Batcher;
import org.hibernate.jdbc.ConnectionManager;
import org.hibernate.stat.CollectionStatistics;
import org.hibernate.loader.collection.BatchingCollectionInitializer;
import org.hibernate.persister.collection.AbstractCollectionPersister;

/**
 * Tests loading of many-to-many collection which should trigger
 * a batch load.
 *
 * @author Steve Ebersole
 */
public class BatchedManyToManyTest extends FunctionalTestCase {
	public BatchedManyToManyTest(String string) {
		super( string );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( BatchedManyToManyTest.class );
	}

	public String[] getMappings() {
		return new String[] { "manytomany/batchload/UserGroupBatchLoad.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.BATCH_STRATEGY, TestingBatcherFactory.class.getName() );
	}

	public static class TestingBatcherFactory implements BatcherFactory {
		public Batcher createBatcher(ConnectionManager connectionManager, Interceptor interceptor) {
			return new TestingBatcher( connectionManager, interceptor );
		}
	}

	public static class TestingBatcher extends NonBatchingBatcher {
		public TestingBatcher(ConnectionManager connectionManager, Interceptor interceptor) {
			super( connectionManager, interceptor );
		}

	}

	public void testProperLoaderSetup() {
		AbstractCollectionPersister cp = ( AbstractCollectionPersister )
				sfi().getCollectionPersister( User.class.getName() + ".groups" );
		assertClassAssignability( BatchingCollectionInitializer.class, cp.getInitializer().getClass() );
		BatchingCollectionInitializer initializer = ( BatchingCollectionInitializer ) cp.getInitializer();
		assertEquals( 50, findMaxBatchSize( initializer.getBatchSizes() ) );
	}

	private int findMaxBatchSize(int[] batchSizes) {
		int max = 0;
		for ( int size : batchSizes ) {
			if ( size > max ) {
				max = size;
			}
		}
		return max;
	}

	public void testLoadingNonInverseSide() {
		prepareTestData();

		sfi().getStatistics().clear();
		CollectionStatistics userGroupStats = sfi().getStatistics()
				.getCollectionStatistics( User.class.getName() + ".groups" );
		CollectionStatistics groupUserStats = sfi().getStatistics()
				.getCollectionStatistics( Group.class.getName() + ".users" );

		Interceptor testingInterceptor = new EmptyInterceptor() {
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

		cleanupTestData();
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
}
