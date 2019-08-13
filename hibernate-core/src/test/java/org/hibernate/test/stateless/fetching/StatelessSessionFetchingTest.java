/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.stateless.fetching;

import java.util.Date;
import java.util.Locale;

import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.StringHelper;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class StatelessSessionFetchingTest extends BaseCoreFunctionalTestCase {

	@Override
	public String[] getMappings() {
		return new String[] { "stateless/fetching/Mappings.hbm.xml" };
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Producer.class, Product.class, Vendor.class };
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setPhysicalNamingStrategy( new TestingNamingStrategy() );
	}

	private class TestingNamingStrategy extends PhysicalNamingStrategyStandardImpl {
		private final String prefix = determineUniquePrefix();

		protected String applyPrefix(String baseTableName) {
			String prefixed = prefix + '_' + baseTableName;
            log.debug("prefixed table name : " + baseTableName + " -> " + prefixed);
			return prefixed;
		}

		@Override
		public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
			return jdbcEnvironment.getIdentifierHelper().toIdentifier( applyPrefix( name.getText() ) );
		}

		private String determineUniquePrefix() {
			return StringHelper.collapseQualifier( getClass().getName(), false ).toUpperCase(Locale.ROOT);
		}
	}

	@Test
	public void testDynamicFetch() {
		Session s = openSession();
		s.beginTransaction();
		Date now = new Date();
		User me = new User( "me" );
		User you = new User( "you" );
		Resource yourClock = new Resource( "clock", you );
		Task task = new Task( me, "clean", yourClock, now ); // :)
		s.save( me );
		s.save( you );
		s.save( yourClock );
		s.save( task );
		s.getTransaction().commit();
		s.close();

		StatelessSession ss = sessionFactory().openStatelessSession();
		ss.beginTransaction();
		Task taskRef = ( Task ) ss.createQuery( "from Task t join fetch t.resource join fetch t.user" ).uniqueResult();
		assertTrue( taskRef != null );
		assertTrue( Hibernate.isInitialized( taskRef ) );
		assertTrue( Hibernate.isInitialized( taskRef.getUser() ) );
		assertTrue( Hibernate.isInitialized( taskRef.getResource() ) );
		assertFalse( Hibernate.isInitialized( taskRef.getResource().getOwner() ) );
		ss.getTransaction().commit();
		ss.close();

		cleanup();
	}

	@Test
	public void testDynamicFetchScroll() {
		Session s = openSession();
		s.beginTransaction();
		Date now = new Date();

		User me = new User( "me" );
		User you = new User( "you" );
		Resource yourClock = new Resource( "clock", you );
		Task task = new Task( me, "clean", yourClock, now ); // :)

		s.save( me );
		s.save( you );
		s.save( yourClock );
		s.save( task );

		User u3 = new User( "U3" );
		User u4 = new User( "U4" );
		Resource it = new Resource( "it", u4 );
		Task task2 = new Task( u3, "beat", it, now ); // :))

		s.save( u3 );
		s.save( u4 );
		s.save( it );
		s.save( task2 );

		s.getTransaction().commit();
		s.close();

		StatelessSession ss = sessionFactory().openStatelessSession();
		ss.beginTransaction();

		final Query query = ss.createQuery( "from Task t join fetch t.resource join fetch t.user");
		final ScrollableResults scrollableResults = query.scroll( ScrollMode.FORWARD_ONLY);
		while ( scrollableResults.next() ) {
			Task taskRef = (Task) scrollableResults.get( 0 );
			assertTrue( Hibernate.isInitialized( taskRef ) );
			assertTrue( Hibernate.isInitialized( taskRef.getUser() ) );
			assertTrue( Hibernate.isInitialized( taskRef.getResource() ) );
			assertFalse( Hibernate.isInitialized( taskRef.getResource().getOwner() ) );
		}

		ss.getTransaction().commit();
		ss.close();

		cleanup();
	}

	@Test
	public void testDynamicFetchScrollSession() {
		Session s = openSession();
		s.beginTransaction();
		Date now = new Date();

		User me = new User( "me" );
		User you = new User( "you" );
		Resource yourClock = new Resource( "clock", you );
		Task task = new Task( me, "clean", yourClock, now ); // :)

		s.save( me );
		s.save( you );
		s.save( yourClock );
		s.save( task );

		User u3 = new User( "U3" );
		User u4 = new User( "U4" );
		Resource it = new Resource( "it", u4 );
		Task task2 = new Task( u3, "beat", it, now ); // :))

		s.save( u3 );
		s.save( u4 );
		s.save( it );
		s.save( task2 );

		s.getTransaction().commit();
		s.close();

		inTransaction(
				session -> {
					final Query query = session.createQuery( "from Task t join fetch t.resource join fetch t.user");
					final ScrollableResults scrollableResults = query.scroll( ScrollMode.FORWARD_ONLY );
					while ( scrollableResults.next() ) {
						Task taskRef = (Task) scrollableResults.get( 0 );
						assertTrue( Hibernate.isInitialized( taskRef ) );
						assertTrue( Hibernate.isInitialized( taskRef.getUser() ) );
						assertTrue( Hibernate.isInitialized( taskRef.getResource() ) );
						assertFalse( Hibernate.isInitialized( taskRef.getResource().getOwner() ) );
					}

				}
		);

		cleanup();
	}

	@Test
	public void testDynamicFetchCollectionScroll() {
		Session s = openSession();
		s.beginTransaction();

		Producer p1 = new Producer( 1, "Acme" );
		Producer p2 = new Producer( 2, "ABC" );

		session.save( p1 );
		session.save( p2 );

		Vendor v1 = new Vendor( 1, "v1" );
		Vendor v2 = new Vendor( 2, "v2" );

		session.save( v1 );
		session.save( v2 );

		final Product product1 = new Product(1, "123", v1, p1);
		final Product product2 = new Product(2, "456", v1, p1);
		final Product product3 = new Product(3, "789", v1, p2);

		session.save( product1 );
		session.save( product2 );
		session.save( product3 );

		s.getTransaction().commit();
		s.close();

		StatelessSession ss = sessionFactory().openStatelessSession();
		ss.beginTransaction();

		final Query query = ss.createQuery( "select p from Producer p join fetch p.products" );
		ScrollableResults scrollableResults = null;
		if ( getDialect() instanceof DB2Dialect ) {
			/*
				FetchingScrollableResultsImp#next() in order to check if the ResultSet is empty calls ResultSet#isBeforeFirst()
				but the support for ResultSet#isBeforeFirst() is optional for ResultSets with a result
				set type of TYPE_FORWARD_ONLY and db2 does not support it.
			 */
			scrollableResults = query.scroll( ScrollMode.SCROLL_INSENSITIVE );
		}
		else {
			scrollableResults = query.scroll( ScrollMode.FORWARD_ONLY );
		}
		while ( scrollableResults.next() ) {
			Producer producer = (Producer) scrollableResults.get( 0 );
			assertTrue( Hibernate.isInitialized( producer ) );
			assertTrue( Hibernate.isInitialized( producer.getProducts() ) );

			for (Product product : producer.getProducts()) {
				assertTrue( Hibernate.isInitialized( product ) );
				assertFalse( Hibernate.isInitialized( product.getVendor() ) );
			}
		}

		ss.getTransaction().commit();
		ss.close();

		cleanup();
	}

	private void cleanup() {
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( "delete Task" ).executeUpdate();
		s.createQuery( "delete Resource" ).executeUpdate();
		s.createQuery( "delete User" ).executeUpdate();

		s.createQuery( "delete Product" ).executeUpdate();
		s.createQuery( "delete Producer" ).executeUpdate();
		s.createQuery( "delete Vendor" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}
}
