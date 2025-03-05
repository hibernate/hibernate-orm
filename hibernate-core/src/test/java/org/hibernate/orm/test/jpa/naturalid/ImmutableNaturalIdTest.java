/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.naturalid;

import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;

import org.hibernate.orm.test.jpa.model.AbstractJPATest;
import org.junit.jupiter.api.Test;

import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * copied from {@link org.hibernate.orm.test.mapping.naturalid.immutable.ImmutableNaturalIdTest}
 *
 * @author Steve Ebersole
 */
public class ImmutableNaturalIdTest extends AbstractJPATest {
	@Override
	protected String[] getOrmXmlFiles() {
		return new String[] { "org/hibernate/orm/test/jpa/naturalid/User.hbm.xml" };
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builder) {
		super.applySettings( builder );
		builder.applySetting( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		builder.applySetting( Environment.USE_QUERY_CACHE, "true" );
		builder.applySetting( Environment.GENERATE_STATISTICS, "true" );
	}

	@Test
	public void testMerge() {
		// prepare some test data...
		User user = new User();
		inTransaction(
				session -> {
					user.setUserName( "steve" );
					user.setEmail( "steve@hibernate.org" );
					user.setPassword( "brewhaha" );
					session.persist( user );
				}
		);

		// 'user' is now a detached entity, so lets change a property and reattch...
		user.setPassword( "homebrew" );
		User merged = fromTransaction(
				session ->
						session.merge( user )
		);

		// clean up
		inTransaction(
				session ->
						session.remove( session.getReference(merged) )
		);
	}

	@Test
	public void testNaturalIdCheck()  {
		sessionFactoryScope().inSession(
				session -> {
					Transaction t = session.beginTransaction();
					User u = new User( "steve", "superSecret" );
					session.persist( u );
					u.setUserName( "Steve" );
					try {
						session.flush();
						fail( "PersistenceException expected" );
					}
					catch (PersistenceException p) {
						//expected
						t.rollback();
					}
					session.close();
				}
		);

	}

	@Test
	public void testSimpleNaturalIdLoadAccessCache() {
		inTransaction(
				session -> {
					User u = new User( "steve", "superSecret" );
					session.persist( u );
				}
		);

		inTransaction(
				session -> {
					User u = session.bySimpleNaturalId( User.class ).load( "steve" );
					assertNotNull( u );
					User u2 = session.bySimpleNaturalId( User.class ).getReference( "steve" );
					assertSame( u2, u );
				}
		);

		inTransaction(
				session ->
						session.createQuery( "delete User" ).executeUpdate()
		);
	}

	@Test
	public void testNaturalIdLoadAccessCache() {
		inTransaction(
				session -> {
					User u = new User( "steve", "superSecret" );
					session.persist( u );
				}
		);

		sessionFactory().getStatistics().clear();

		inTransaction(
				session -> {
					User u = (User) session.byNaturalId( User.class ).using( "userName", "steve" ).load();
					assertNotNull( u );
				}
		);

		assertEquals( 1, sessionFactory().getStatistics().getEntityLoadCount() );
		assertEquals( 0, sessionFactory().getStatistics().getSecondLevelCacheMissCount() );
		assertEquals( 0, sessionFactory().getStatistics().getSecondLevelCacheHitCount() );
		assertEquals( 0, sessionFactory().getStatistics().getSecondLevelCachePutCount() );
		assertEquals( 1, sessionFactory().getStatistics().getNaturalIdQueryExecutionCount() );
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCacheHitCount() );
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCachePutCount() );

		inTransaction(
				session -> {
					User v = new User( "gavin", "supsup" );
					session.persist( v );
				}
		);

		sessionFactory().getStatistics().clear();

		inTransaction(
				session -> {
					User u = session.byNaturalId( User.class ).using( "userName", "steve" ).load();
					assertNotNull( u );
					assertEquals( 1, sessionFactory().getStatistics().getEntityLoadCount() );
					assertEquals(
							1,
							sessionFactory().getStatistics().getNaturalIdQueryExecutionCount()
					);//0: incorrect stats since hbm.xml can't enable NaturalId caching
					assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCacheHitCount() );
					u = session.byNaturalId( User.class ).using( "userName", "steve" ).load();
					assertNotNull( u );
					assertEquals( 1, sessionFactory().getStatistics().getEntityLoadCount() );
					assertEquals(
							1,
							sessionFactory().getStatistics().getNaturalIdQueryExecutionCount()
					);//0: incorrect stats since hbm.xml can't enable NaturalId caching
					assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCacheHitCount() );

				}
		);

		inTransaction(
				session ->
						session.createQuery( "delete User" ).executeUpdate()
		);
	}
}
