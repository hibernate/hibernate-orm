/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interceptor;

import org.hibernate.AssertionFailure;
import org.hibernate.Interceptor;
import org.hibernate.TransactionException;
import org.hibernate.resource.jdbc.internal.EmptyStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.Type;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 * @author Lukasz Antoniak
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = {
		"org/hibernate/orm/test/interceptor/User.hbm.xml",
		"org/hibernate/orm/test/interceptor/Image.hbm.xml"
})
@SessionFactory
public class InterceptorTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testCollectionIntercept(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(
				factory -> factory.withOptions().interceptor( new CollectionInterceptor() ).openSession(),
				session -> {
					var user = new User( "Gavin", "nivag" );
					session.persist( user );
					user.setPassword( "vagni" );
				}
		);

		factoryScope.inTransaction( session -> {
			var user = session.find( User.class, "Gavin" );
			assertEquals( 2, user.getActions().size() );
			session.remove( user );
		} );
	}

	@Test
	public void testPropertyIntercept(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(
				factory -> factory.withOptions().interceptor( new PropertyInterceptor() ).openSession(),
				session -> {
					var user = new User( "Gavin", "nivag" );
					session.persist( user );
					user.setPassword( "vagni" );
				}
		);

		factoryScope.inTransaction( session -> {
			var user = session.find( User.class, "Gavin" );
			assertNotNull( user.getCreated() );
			assertNotNull( user.getLastUpdated() );
			session.remove( user );
		} );
	}

	/**
	 * Test case from HHH-1921.  Here the interceptor resets the
	 * current-state to the same thing as the current db state; this
	 * causes EntityPersister.findDirty() to return no dirty properties.
	 */
	@Test
	@JiraKey(value = "HHH-1921")
	public void testPropertyIntercept2(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			var user = new User( "Josh", "test" );
			session.persist( user );
		} );

		factoryScope.inTransaction(
				factory -> factory.withOptions().interceptor(
						new Interceptor() {
							@Override
							public boolean onFlushDirty(
									Object entity,
									Object id,
									Object[] currentState,
									Object[] previousState,
									String[] propertyNames,
									Type[] types) {
								for ( int i = 0; i < propertyNames.length; i++ ) {
									if ( propertyNames[i].equals( "password" ) ) {
										currentState[i] = "test";
									}
								}

								return true;
							}
						} ).openSession(),
				session -> {
					var user = session.find( User.class, "Josh" );
					user.setPassword( "nottest" );
				}
		);

		factoryScope.inTransaction( session -> {
			var user = session.find( User.class, "Josh" );
			assertEquals( "test", user.getPassword() );
			session.remove( user );
		} );

	}

	@Test
	public void testComponentInterceptor(SessionFactoryScope factoryScope) {
		final int checkPerm = 500;
		final String checkComment = "generated from interceptor";

		factoryScope.inTransaction(
				factory -> factory.withOptions().interceptor(
						new Interceptor() {
							@Override
							public boolean onPersist(
									Object entity,
									Object id,
									Object[] state,
									String[] propertyNames,
									Type[] types) {
								if ( state[0] == null ) {
									var detail = new Image.Details();
									detail.setPerm1( checkPerm );
									detail.setComment( checkComment );
									state[0] = detail;
								}
								return true;
							}
						}
				).openSession(),
				session -> {
					var image = new Image();
					image.setName( "compincomp" );
					image = session.merge( image );
					assertNotNull( image.getDetails() );
					assertEquals( checkPerm, image.getDetails().getPerm1() );
					assertEquals( checkComment, image.getDetails().getComment() );
				}
		);

		factoryScope.inTransaction( session -> {
			var image = session.find( Image.class, 1L );
			assertNotNull( image.getDetails() );
			assertEquals( checkPerm, image.getDetails().getPerm1() );
			assertEquals( checkComment, image.getDetails().getComment() );
			session.remove( image );
		} );
	}

	@Test
	public void testStatefulIntercept(SessionFactoryScope factoryScope) {
		final var user = new User( "Gavin", "nivag" );
		final var statefulInterceptor = new StatefulInterceptor();

		factoryScope.inTransaction(
				factory -> factory.withOptions().interceptor( statefulInterceptor ).openSession(),
				session -> {
					session.persist( user );
					user.setPassword( "vagni" );
				}
		);

		factoryScope.inTransaction( session -> {
			for ( var log : statefulInterceptor.drainLogs() ) {
				session.persist( log );
			}
		} );

		factoryScope.inTransaction( session -> {
			var criteriaBuilder = session.getCriteriaBuilder();
			var criteria = criteriaBuilder.createQuery( Log.class );
			criteria.from( Log.class );
			var logs = session.createQuery( criteria ).list();
			assertEquals( 2, logs.size() );
			session.remove( user );
			session.createMutationQuery( "delete from Log" ).executeUpdate();
		} );
	}

	@Test
	public void testInitiateIntercept(SessionFactoryScope factoryScope) {
		final String injectedString = "******";
		final var initiateInterceptor = new InstantiateInterceptor( injectedString );
		var created = factoryScope.fromTransaction(
				factory -> factory.withOptions().interceptor( initiateInterceptor ).openSession(),
				session -> {
					var user = new User( "Gavin", "nivag" );
					session.persist( user );
					assertNull( user.getInjectedString() );
					return user;
				}
		);

		created.setPassword( "blah" );

		factoryScope.inTransaction(
				factory -> factory.withOptions().interceptor( initiateInterceptor ).openSession(),
				session -> {
					var merged = session.merge( created );
					assertEquals( injectedString, merged.getInjectedString() );
					assertEquals( created.getName(), merged.getName() );
					assertEquals( created.getPassword(), merged.getPassword() );

					merged.setInjectedString( null );

					var loaded = session.getReference( User.class, merged.getName() );
					// the session-bound instance was not instantiated by the interceptor, load simply returns it
					assertSame( merged, loaded );
					assertNull( merged.getInjectedString() );

					// flush the session and evict the merged instance from session to force an actual load
					session.flush();
					session.evict( merged );

					var reloaded = session.getReference( User.class, merged.getName() );
					// Interceptor IS called for instantiating the persistent instance associated to the session when using load
					assertEquals( injectedString, reloaded.getInjectedString() );
					assertEquals( created.getName(), reloaded.getName() );
					assertEquals( created.getPassword(), reloaded.getPassword() );

					session.remove( reloaded );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-6594")
	public void testPrepareStatementIntercept(SessionFactoryScope factoryScope) {
		final Queue<String> expectedSQLs = new LinkedList<>();
		// Transaction 1
		expectedSQLs.add( "insert" );
		// Transaction 2
		expectedSQLs.add( "select" );
		expectedSQLs.add( "select" );
		// Transaction 3
		expectedSQLs.add( "select" );
//		expectedSQLs.add( "select" );
		expectedSQLs.add( "update" );
		// Transaction 4
		expectedSQLs.add( "select" );
		expectedSQLs.add( "delete" );

		final var statementInspector = new EmptyStatementInspector() {
			@Override
			public String inspect(String sql) {
				assertNotNull( sql );
				String expectedSql = expectedSQLs.poll().toLowerCase( Locale.ROOT );
				assertTrue(
						sql.toLowerCase( Locale.ROOT ).startsWith( expectedSql ),
						"sql:\n " + sql.toLowerCase( Locale.ROOT ) + "\n doesn't start with \n" + expectedSql + "\n"

				);
				return sql;
			}
		};

		var sessionFactory = factoryScope.getSessionFactory();

		var session = sessionFactory.withOptions().statementInspector( statementInspector ).openSession();
		var transaction = session.beginTransaction();
		User user = new User( "Lukasz", "Antoniak" );
		session.persist( user );
		transaction.commit();
		session.close();

		session = sessionFactory.withOptions().statementInspector( statementInspector ).openSession();
		transaction = session.beginTransaction();
		session.get( User.class, "Lukasz" );
		session.createQuery( "from User user", User.class ).list();
		transaction.commit();
		session.close();

		user.setPassword( "Kinga" );
		session = sessionFactory.withOptions().statementInspector( statementInspector ).openSession();
		transaction = session.beginTransaction();
		session.merge( user );
		transaction.commit();
		session.close();

		session = sessionFactory.withOptions().statementInspector( statementInspector ).openSession();
		transaction = session.beginTransaction();
		session.remove( user );
		transaction.commit();
		session.close();

		assertTrue( expectedSQLs.isEmpty() );
	}

	@Test
	public void testPrepareStatementFaultIntercept(SessionFactoryScope factoryScope) {
		final var statementInspector = new EmptyStatementInspector() {
			@Override
			public String inspect(String sql) {
				return null;
			}
		};

		try (var session =
					factoryScope.getSessionFactory().withOptions()
							.statementInspector( statementInspector )
							.openSession()) {
			try {
				var transaction = session.beginTransaction();
				var user = new User( "Kinga", "Mroz" );
				session.persist( user );
				transaction.commit();
			}
			catch (TransactionException e) {
				assertInstanceOf( AssertionFailure.class, e.getCause() );
			}
			finally {
				session.close();
			}
		}
	}
}
