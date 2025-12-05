/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interceptor;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import org.hibernate.AssertionFailure;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.resource.jdbc.internal.EmptyStatementInspector;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.Type;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertNotNull;
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
				(sf) -> sf.withOptions().interceptor( new CollectionInterceptor() ).openSession(),
				(s) -> {
					User u = new User( "Gavin", "nivag" );
					s.persist( u );
					u.setPassword( "vagni" );
				}
		);

		factoryScope.inTransaction( (s) -> {
			var u = s.find( User.class, "Gavin" );
			Assertions.assertEquals( 2, u.getActions().size() );
			s.remove( u );
		} );
	}

	@Test
	public void testPropertyIntercept(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(
				(sf) -> sf.withOptions().interceptor( new PropertyInterceptor() ).openSession(),
				(s) -> {
					User u = new User( "Gavin", "nivag" );
					s.persist( u );
					u.setPassword( "vagni" );
				}
		);

		factoryScope.inTransaction( (s) -> {
			var u = s.find( User.class, "Gavin" );
			assertNotNull( u.getCreated() );
			assertNotNull( u.getLastUpdated() );
			s.remove( u );
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
		factoryScope.inTransaction( (s) -> {
			User u = new User( "Josh", "test" );
			s.persist( u );
		} );

		factoryScope.inTransaction(
				(sf) -> sf.withOptions().interceptor(
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
				(s) -> {
					var u = s.find( User.class, "Josh" );
					u.setPassword( "nottest" );
				}
		);

		factoryScope.inTransaction( (s) -> {
			var u = s.find( User.class, "Josh" );
			Assertions.assertEquals( "test", u.getPassword() );
			s.remove( u );
		} );

	}

	@Test
	public void testComponentInterceptor(SessionFactoryScope factoryScope) {
		final int checkPerm = 500;
		final String checkComment = "generated from interceptor";

		factoryScope.inTransaction(
				(sf) -> sf.withOptions().interceptor(
						new Interceptor() {
							@Override
							public boolean onPersist(
									Object entity,
									Object id,
									Object[] state,
									String[] propertyNames,
									Type[] types) {
								if ( state[0] == null ) {
									Image.Details detail = new Image.Details();
									detail.setPerm1( checkPerm );
									detail.setComment( checkComment );
									state[0] = detail;
								}
								return true;
							}
						}
				).openSession(),
				(s) -> {
					Image i = new Image();
					i.setName( "compincomp" );
					i = s.merge( i );
					assertNotNull( i.getDetails() );
					Assertions.assertEquals( checkPerm, i.getDetails().getPerm1() );
					Assertions.assertEquals( checkComment, i.getDetails().getComment() );
				}
		);

		factoryScope.inTransaction( (s) -> {
			var i = s.find( Image.class, 1L );
			assertNotNull( i.getDetails() );
			Assertions.assertEquals( checkPerm, i.getDetails().getPerm1() );
			Assertions.assertEquals( checkComment, i.getDetails().getComment() );
			s.remove( i );
		} );
	}

	@Test
	public void testStatefulIntercept(SessionFactoryScope factoryScope) {
		final User u = new User( "Gavin", "nivag" );
		final StatefulInterceptor statefulInterceptor = new StatefulInterceptor();

		factoryScope.inTransaction(
				(sf) -> sf.withOptions().interceptor( statefulInterceptor ).openSession(),
				(s) -> {
					statefulInterceptor.setSession( s );
					s.persist( u );
					u.setPassword( "vagni" );
				}
		);

		factoryScope.inTransaction( s -> {
			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<Log> criteria = criteriaBuilder.createQuery( Log.class );
			criteria.from( Log.class );
			List<Log> logs = s.createQuery( criteria ).list();
//		List logs = s.createCriteria(Log.class).list();
			Assertions.assertEquals( 2, logs.size() );
			s.remove( u );
			s.createMutationQuery( "delete from Log" ).executeUpdate();
		} );
	}

	@Test
	public void testInitiateIntercept(SessionFactoryScope factoryScope) {
		final String injectedString = "******";
		final InstantiateInterceptor initiateInterceptor = new InstantiateInterceptor( injectedString );
		var created = factoryScope.fromTransaction(
				(sf) -> sf.withOptions().interceptor( initiateInterceptor ).openSession(),
				(s) -> {
					User u = new User( "Gavin", "nivag" );
					s.persist( u );
					Assertions.assertNull( u.getInjectedString() );
					return u;
				}
		);

		created.setPassword( "blah" );

		factoryScope.inTransaction(
				(sf) -> sf.withOptions().interceptor( initiateInterceptor ).openSession(),
				(s) -> {
					User merged = s.merge( created );
					Assertions.assertEquals( injectedString, merged.getInjectedString() );
					Assertions.assertEquals( created.getName(), merged.getName() );
					Assertions.assertEquals( created.getPassword(), merged.getPassword() );

					merged.setInjectedString( null );

					User loaded = s.getReference( User.class, merged.getName() );
					// the session-bound instance was not instantiated by the interceptor, load simply returns it
					Assertions.assertSame( merged, loaded );
					Assertions.assertNull( merged.getInjectedString() );

					// flush the session and evict the merged instance from session to force an actual load
					s.flush();
					s.evict( merged );

					User reloaded = s.getReference( User.class, merged.getName() );
					// Interceptor IS called for instantiating the persistent instance associated to the session when using load
					Assertions.assertEquals( injectedString, reloaded.getInjectedString() );
					Assertions.assertEquals( created.getName(), reloaded.getName() );
					Assertions.assertEquals( created.getPassword(), reloaded.getPassword() );

					s.remove( reloaded );
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

		final StatementInspector statementInspector = new EmptyStatementInspector() {
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

		Session s = factoryScope.getSessionFactory().withOptions().statementInspector( statementInspector ).openSession();
		Transaction t = s.beginTransaction();
		User u = new User( "Lukasz", "Antoniak" );
		s.persist( u );
		t.commit();
		s.close();

		s = factoryScope.getSessionFactory().withOptions().statementInspector( statementInspector ).openSession();
		t = s.beginTransaction();
		s.get( User.class, "Lukasz" );
		s.createQuery( "from User u" ).list();
		t.commit();
		s.close();

		u.setPassword( "Kinga" );
		s = factoryScope.getSessionFactory().withOptions().statementInspector( statementInspector ).openSession();
		t = s.beginTransaction();
		s.merge( u );
		t.commit();
		s.close();

		s = factoryScope.getSessionFactory().withOptions().statementInspector( statementInspector ).openSession();
		t = s.beginTransaction();
		s.remove( u );
		t.commit();
		s.close();

		assertTrue( expectedSQLs.isEmpty() );
	}

	@Test
	public void testPrepareStatementFaultIntercept(SessionFactoryScope factoryScope) {
		final StatementInspector statementInspector = new EmptyStatementInspector() {
			@Override
			public String inspect(String sql) {
				return null;
			}
		};

		try (var s = factoryScope.getSessionFactory().withOptions().statementInspector( statementInspector ).openSession()) {
			try {
				Transaction t = s.beginTransaction();
				User u = new User( "Kinga", "Mroz" );
				s.persist( u );
				t.commit();
			}
			catch (TransactionException e) {
				Assertions.assertInstanceOf( AssertionFailure.class, e.getCause() );
			}
			finally {
				s.close();
			}
		}
	}
}
