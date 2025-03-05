/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.readonly;

import java.math.BigDecimal;

import org.hibernate.CacheMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * Tests making initialized and uninitialized proxies read-only/modifiable
 *
 * @author Gail Badner
 */
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/readonly/DataPoint.hbm.xml",
				"org/hibernate/orm/test/readonly/TextHolder.hbm.xml"
		}
)
public class ReadOnlyProxyTest extends AbstractReadOnlyTest {

	@Test
	public void testReadOnlyViaSessionDoesNotInit(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						checkReadOnly( session, dp, false );
						assertFalse( Hibernate.isInitialized( dp ) );
						session.setReadOnly( dp, true );
						checkReadOnly( session, dp, true );
						assertFalse( Hibernate.isInitialized( dp ) );
						session.setReadOnly( dp, false );
						checkReadOnly( session, dp, false );
						assertFalse( Hibernate.isInitialized( dp ) );
						session.flush();
						checkReadOnly( session, dp, false );
						assertFalse( Hibernate.isInitialized( dp ) );
						session.getTransaction().commit();
						checkReadOnly( session, dp, false );
						assertFalse( Hibernate.isInitialized( dp ) );
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inTransaction(
				session -> {
					DataPoint dp = session.get( DataPoint.class, dpOrig.getId() );
					assertEquals( dpOrig.getId(), dp.getId() );
					assertEquals( dpOrig.getDescription(), dp.getDescription() );
					assertEquals( dpOrig.getX(), dp.getX() );
					assertEquals( dpOrig.getY(), dp.getY() );
					session.remove( dp );
				}
		);
	}

	@Test
	public void testReadOnlyViaLazyInitializerDoesNotInit(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						LazyInitializer dpLI = ( (HibernateProxy) dp ).getHibernateLazyInitializer();
						checkReadOnly( session, dp, false );
						assertFalse( Hibernate.isInitialized( dp ) );
						dpLI.setReadOnly( true );
						checkReadOnly( session, dp, true );
						assertFalse( Hibernate.isInitialized( dp ) );
						dpLI.setReadOnly( false );
						checkReadOnly( session, dp, false );
						assertFalse( Hibernate.isInitialized( dp ) );
						session.flush();
						checkReadOnly( session, dp, false );
						assertFalse( Hibernate.isInitialized( dp ) );
						session.getTransaction().commit();
						checkReadOnly( session, dp, false );
						assertFalse( Hibernate.isInitialized( dp ) );
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inTransaction(
				session -> {
					DataPoint dp = session.get( DataPoint.class, dpOrig.getId() );
					assertEquals( dpOrig.getId(), dp.getId() );
					assertEquals( dpOrig.getDescription(), dp.getDescription() );
					assertEquals( dpOrig.getX(), dp.getX() );
					assertEquals( dpOrig.getY(), dp.getY() );
					session.remove( dp );
				}
		);
	}

	@Test
	public void testReadOnlyViaSessionNoChangeAfterInit(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						checkReadOnly( session, dp, false );
						assertFalse( Hibernate.isInitialized( dp ) );
						Hibernate.initialize( dp );
						assertTrue( Hibernate.isInitialized( dp ) );
						checkReadOnly( session, dp, false );
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						session.setReadOnly( dp, true );
						checkReadOnly( session, dp, true );
						assertFalse( Hibernate.isInitialized( dp ) );
						Hibernate.initialize( dp );
						assertTrue( Hibernate.isInitialized( dp ) );
						checkReadOnly( session, dp, true );
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						session.setReadOnly( dp, true );
						checkReadOnly( session, dp, true );
						assertFalse( Hibernate.isInitialized( dp ) );
						session.setReadOnly( dp, false );
						checkReadOnly( session, dp, false );
						assertFalse( Hibernate.isInitialized( dp ) );
						Hibernate.initialize( dp );
						assertTrue( Hibernate.isInitialized( dp ) );
						checkReadOnly( session, dp, false );
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inTransaction(
				session -> {
					DataPoint dp = session.get( DataPoint.class, dpOrig.getId() );
					assertEquals( dpOrig.getId(), dp.getId() );
					assertEquals( dpOrig.getDescription(), dp.getDescription() );
					assertEquals( dpOrig.getX(), dp.getX() );
					assertEquals( dpOrig.getY(), dp.getY() );
					session.remove( dp );
				}
		);
	}

	@Test
	public void testReadOnlyViaLazyInitializerNoChangeAfterInit(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						LazyInitializer dpLI = ( (HibernateProxy) dp ).getHibernateLazyInitializer();
						checkReadOnly( session, dp, false );
						assertTrue( dpLI.isUninitialized() );
						Hibernate.initialize( dp );
						assertFalse( dpLI.isUninitialized() );
						checkReadOnly( session, dp, false );
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						LazyInitializer dpLI = ( (HibernateProxy) dp ).getHibernateLazyInitializer();
						dpLI.setReadOnly( true );
						checkReadOnly( session, dp, true );
						assertTrue( dpLI.isUninitialized() );
						Hibernate.initialize( dp );
						assertFalse( dpLI.isUninitialized() );
						checkReadOnly( session, dp, true );
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						LazyInitializer dpLI = ( (HibernateProxy) dp ).getHibernateLazyInitializer();
						dpLI.setReadOnly( true );
						checkReadOnly( session, dp, true );
						assertTrue( dpLI.isUninitialized() );
						dpLI.setReadOnly( false );
						checkReadOnly( session, dp, false );
						assertTrue( dpLI.isUninitialized() );
						Hibernate.initialize( dp );
						assertFalse( dpLI.isUninitialized() );
						checkReadOnly( session, dp, false );
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inTransaction(
				session -> {
					DataPoint dp = session.get( DataPoint.class, dpOrig.getId() );
					assertEquals( dpOrig.getId(), dp.getId() );
					assertEquals( dpOrig.getDescription(), dp.getDescription() );
					assertEquals( dpOrig.getX(), dp.getX() );
					assertEquals( dpOrig.getY(), dp.getY() );
					session.remove( dp );
				}
		);
	}

	@Test
	public void testReadOnlyViaSessionBeforeInit(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						session.setReadOnly( dp, true );
						dp.setDescription( "changed" );
						assertTrue( Hibernate.isInitialized( dp ) );
						assertEquals( "changed", dp.getDescription() );
						checkReadOnly( session, dp, true );
						session.flush();
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inTransaction(
				session -> {
					DataPoint dp = session.get( DataPoint.class, dpOrig.getId() );
					assertEquals( dpOrig.getId(), dp.getId() );
					assertEquals( dpOrig.getDescription(), dp.getDescription() );
					assertEquals( dpOrig.getX(), dp.getX() );
					assertEquals( dpOrig.getY(), dp.getY() );
					session.remove( dp );
				}
		);
	}

	@Test
	public void testModifiableViaSessionBeforeInit(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						checkReadOnly( session, dp, false );
						dp.setDescription( "changed" );
						assertTrue( Hibernate.isInitialized( dp ) );
						assertEquals( "changed", dp.getDescription() );
						checkReadOnly( session, dp, false );
						session.flush();
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inTransaction(
				session -> {
					DataPoint dp = session.get( DataPoint.class, dpOrig.getId() );
					assertEquals( dpOrig.getId(), dp.getId() );
					assertEquals( "changed", dp.getDescription() );
					assertEquals( dpOrig.getX(), dp.getX() );
					assertEquals( dpOrig.getY(), dp.getY() );
					session.remove( dp );
				}
		);
	}

	@Test
	public void testReadOnlyViaSessionBeforeInitByModifiableQuery(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						assertFalse( Hibernate.isInitialized( dp ) );
						checkReadOnly( session, dp, false );
						session.setReadOnly( dp, true );
						assertFalse( Hibernate.isInitialized( dp ) );
						checkReadOnly( session, dp, true );
						assertFalse( Hibernate.isInitialized( dp ) );
						DataPoint dpFromQuery = (DataPoint) session.createQuery( "from DataPoint where id=" + dpOrig.getId() )
								.setReadOnly(
										false )
								.uniqueResult();
						assertTrue( Hibernate.isInitialized( dpFromQuery ) );
						assertSame( dp, dpFromQuery );
						checkReadOnly( session, dp, true );
						dp.setDescription( "changed" );
						assertEquals( "changed", dp.getDescription() );
						session.flush();
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inTransaction(
				session -> {
					DataPoint dp = session.get( DataPoint.class, dpOrig.getId() );
					assertEquals( dpOrig.getId(), dp.getId() );
					assertEquals( dpOrig.getDescription(), dp.getDescription() );
					assertEquals( dpOrig.getX(), dp.getX() );
					assertEquals( dpOrig.getY(), dp.getY() );
					session.remove( dp );
				}
		);
	}

	@Test
	public void testReadOnlyViaSessionBeforeInitByReadOnlyQuery(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						assertFalse( Hibernate.isInitialized( dp ) );
						checkReadOnly( session, dp, false );
						session.setReadOnly( dp, true );
						checkReadOnly( session, dp, true );
						DataPoint dpFromQuery = (DataPoint) session.createQuery( "from DataPoint where id=" + dpOrig.getId() )
								.setReadOnly(
										true )
								.uniqueResult();
						assertTrue( Hibernate.isInitialized( dpFromQuery ) );
						assertSame( dp, dpFromQuery );
						checkReadOnly( session, dp, true );
						dp.setDescription( "changed" );
						assertEquals( "changed", dp.getDescription() );
						session.flush();
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inTransaction(
				session -> {
					DataPoint dp = session.get( DataPoint.class, dpOrig.getId() );
					assertEquals( dpOrig.getId(), dp.getId() );
					assertEquals( dpOrig.getDescription(), dp.getDescription() );
					assertEquals( dpOrig.getX(), dp.getX() );
					assertEquals( dpOrig.getY(), dp.getY() );
					session.remove( dp );
				}
		);
	}

	@Test
	public void testModifiableViaSessionBeforeInitByModifiableQuery(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						assertFalse( Hibernate.isInitialized( dp ) );
						checkReadOnly( session, dp, false );
						DataPoint dpFromQuery = (DataPoint) session.createQuery( "from DataPoint where id=" + dpOrig.getId() )
								.setReadOnly(
										false )
								.uniqueResult();
						assertTrue( Hibernate.isInitialized( dpFromQuery ) );
						assertSame( dp, dpFromQuery );
						checkReadOnly( session, dp, false );
						dp.setDescription( "changed" );
						assertEquals( "changed", dp.getDescription() );
						session.flush();
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inTransaction(
				session -> {
					DataPoint dp = session.get( DataPoint.class, dpOrig.getId() );
					assertEquals( dpOrig.getId(), dp.getId() );
					assertEquals( "changed", dp.getDescription() );
					assertEquals( dpOrig.getX(), dp.getX() );
					assertEquals( dpOrig.getY(), dp.getY() );
					session.remove( dp );
				}
		);
	}

	@Test
	public void testModifiableViaSessionBeforeInitByReadOnlyQuery(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						checkReadOnly( session, dp, false );
						assertFalse( Hibernate.isInitialized( dp ) );
						DataPoint dpFromQuery = (DataPoint) session.createQuery( "from DataPoint where id=" + dpOrig.getId() )
								.setReadOnly(
										true )
								.uniqueResult();
						assertTrue( Hibernate.isInitialized( dpFromQuery ) );
						assertSame( dp, dpFromQuery );
						checkReadOnly( session, dp, false );
						dp.setDescription( "changed" );
						assertEquals( "changed", dp.getDescription() );
						session.flush();
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inTransaction(
				session -> {
					DataPoint dp = session.get( DataPoint.class, dpOrig.getId() );
					assertEquals( dpOrig.getId(), dp.getId() );
					assertEquals( "changed", dp.getDescription() );
					assertEquals( dpOrig.getX(), dp.getX() );
					assertEquals( dpOrig.getY(), dp.getY() );
					session.remove( dp );
				}
		);
	}

	@Test
	public void testReadOnlyViaLazyInitializerBeforeInit(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						LazyInitializer dpLI = ( (HibernateProxy) dp ).getHibernateLazyInitializer();
						assertTrue( dpLI.isUninitialized() );
						checkReadOnly( session, dp, false );
						dpLI.setReadOnly( true );
						checkReadOnly( session, dp, true );
						dp.setDescription( "changed" );
						assertFalse( dpLI.isUninitialized() );
						assertEquals( "changed", dp.getDescription() );
						checkReadOnly( session, dp, true );
						session.flush();
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inTransaction(
				session -> {
					DataPoint dp = session.get( DataPoint.class, dpOrig.getId() );
					assertEquals( dpOrig.getId(), dp.getId() );
					assertEquals( dpOrig.getDescription(), dp.getDescription() );
					assertEquals( dpOrig.getX(), dp.getX() );
					assertEquals( dpOrig.getY(), dp.getY() );
					session.remove( dp );
				}
		);
	}

	@Test
	public void testModifiableViaLazyInitializerBeforeInit(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						LazyInitializer dpLI = ( (HibernateProxy) dp ).getHibernateLazyInitializer();
						assertTrue( dp instanceof HibernateProxy );
						assertTrue( dpLI.isUninitialized() );
						checkReadOnly( session, dp, false );
						dp.setDescription( "changed" );
						assertFalse( dpLI.isUninitialized() );
						assertEquals( "changed", dp.getDescription() );
						checkReadOnly( session, dp, false );
						session.flush();
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inTransaction(
				session -> {
					DataPoint dp = session.get( DataPoint.class, dpOrig.getId() );
					assertEquals( dpOrig.getId(), dp.getId() );
					assertEquals( "changed", dp.getDescription() );
					assertEquals( dpOrig.getX(), dp.getX() );
					assertEquals( dpOrig.getY(), dp.getY() );
					session.remove( dp );
				}
		);
	}

	@Test
	public void testReadOnlyViaLazyInitializerAfterInit(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						LazyInitializer dpLI = ( (HibernateProxy) dp ).getHibernateLazyInitializer();
						assertTrue( dpLI.isUninitialized() );
						checkReadOnly( session, dp, false );
						dp.setDescription( "changed" );
						assertFalse( dpLI.isUninitialized() );
						assertEquals( "changed", dp.getDescription() );
						checkReadOnly( session, dp, false );
						dpLI.setReadOnly( true );
						checkReadOnly( session, dp, true );
						session.flush();
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inTransaction(
				session -> {
					DataPoint dp = session.get( DataPoint.class, dpOrig.getId() );
					assertEquals( dpOrig.getId(), dp.getId() );
					assertEquals( dpOrig.getDescription(), dp.getDescription() );
					assertEquals( dpOrig.getX(), dp.getX() );
					assertEquals( dpOrig.getY(), dp.getY() );
					session.remove( dp );
				}
		);
	}

	@Test
	public void testModifiableViaLazyInitializerAfterInit(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						LazyInitializer dpLI = ( (HibernateProxy) dp ).getHibernateLazyInitializer();
						assertTrue( dpLI.isUninitialized() );
						checkReadOnly( session, dp, false );
						dp.setDescription( "changed" );
						assertFalse( dpLI.isUninitialized() );
						assertEquals( "changed", dp.getDescription() );
						checkReadOnly( session, dp, false );
						session.flush();
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}

				}
		);

		scope.inTransaction(
				session -> {
					DataPoint dp = session.get( DataPoint.class, dpOrig.getId() );
					assertEquals( dpOrig.getId(), dp.getId() );
					assertEquals( "changed", dp.getDescription() );
					assertEquals( dpOrig.getX(), dp.getX() );
					assertEquals( dpOrig.getY(), dp.getY() );
					session.remove( dp );
				}
		);
	}

	@Test
	@FailureExpected(jiraKey = "HHH-4642")
	public void testModifyToReadOnlyToModifiableIsUpdated(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						assertFalse( Hibernate.isInitialized( dp ) );
						checkReadOnly( session, dp, false );
						dp.setDescription( "changed" );
						assertTrue( Hibernate.isInitialized( dp ) );
						assertEquals( "changed", dp.getDescription() );
						session.setReadOnly( dp, true );
						checkReadOnly( session, dp, true );
						session.setReadOnly( dp, false );
						checkReadOnly( session, dp, false );
						assertEquals( "changed", dp.getDescription() );
						session.flush();
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inSession(
				session -> {
					session.beginTransaction();
					try {
						DataPoint dp = session.get( DataPoint.class, dpOrig.getId() );
						assertEquals( dpOrig.getId(), dp.getId() );
						assertEquals( dpOrig.getDescription(), dp.getDescription() );
						assertEquals( dpOrig.getX(), dp.getX() );
						assertEquals( dpOrig.getY(), dp.getY() );

						assertEquals( "changed", dp.getDescription() );
						// should fail due to HHH-4642
					}
					finally {
						session.getTransaction().rollback();
					}
				}
		);

		scope.inTransaction(
				session -> {
					DataPoint dp = session.get( DataPoint.class, dpOrig.getId() );
					session.remove( dp );
				}
		);
	}

	@Test
	@FailureExpected(jiraKey = "HHH-4642")
	public void testReadOnlyModifiedToModifiableIsUpdated(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						assertFalse( Hibernate.isInitialized( dp ) );
						checkReadOnly( session, dp, false );
						session.setReadOnly( dp, true );
						checkReadOnly( session, dp, true );
						dp.setDescription( "changed" );
						assertTrue( Hibernate.isInitialized( dp ) );
						assertEquals( "changed", dp.getDescription() );
						session.setReadOnly( dp, false );
						checkReadOnly( session, dp, false );
						assertEquals( "changed", dp.getDescription() );
						session.flush();
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inSession(
				session -> {
					session.beginTransaction();
					try {
						DataPoint dp = session.get( DataPoint.class, dpOrig.getId() );
						assertEquals( dpOrig.getId(), dp.getId() );
						assertEquals( dpOrig.getDescription(), dp.getDescription() );
						assertEquals( dpOrig.getX(), dp.getX() );
						assertEquals( dpOrig.getY(), dp.getY() );

						assertEquals( "changed", dp.getDescription() );
						// should fail due to HHH-4642
					}
					finally {
						session.getTransaction().rollback();
					}
				}
		);

		scope.inTransaction(
				session -> {
					DataPoint dp = session.get( DataPoint.class, dpOrig.getId() );
					session.remove( dp );
				}
		);
	}

	@Test
	public void testReadOnlyChangedEvictedMerge(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						assertFalse( Hibernate.isInitialized( dp ) );
						checkReadOnly( session, dp, false );
						session.setReadOnly( dp, true );
						checkReadOnly( session, dp, true );
						dp.setDescription( "changed" );
						assertTrue( Hibernate.isInitialized( dp ) );
						assertEquals( "changed", dp.getDescription() );
						session.evict( dp );
						assertFalse( session.contains( dp ) );
						DataPoint merged = session.merge( dp );
						assertEquals( false, session.isReadOnly( merged ));
						assertEquals( "changed", merged.getDescription() );
						session.flush();
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}

				}
		);

		scope.inTransaction(
				session -> {
					DataPoint dp = session.get( DataPoint.class, dpOrig.getId() );
					assertEquals( "changed", dp.getDescription() );
					assertEquals( dpOrig.getId(), dp.getId() );
					assertEquals( dpOrig.getX(), dp.getX() );
					assertEquals( dpOrig.getY(), dp.getY() );
					session.remove( dp );
				}
		);
	}

	@Test
	public void testReadOnlyToModifiableInitWhenModifiedIsUpdated(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						checkReadOnly( session, dp, false );
						session.setReadOnly( dp, true );
						checkReadOnly( session, dp, true );
						session.setReadOnly( dp, false );
						checkReadOnly( session, dp, false );
						assertFalse( Hibernate.isInitialized( dp ) );
						dp.setDescription( "changed" );
						assertTrue( Hibernate.isInitialized( dp ) );
						assertEquals( "changed", dp.getDescription() );
						session.flush();
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inTransaction(
				session -> {
					DataPoint dp = session.get( DataPoint.class, dpOrig.getId() );
					assertEquals( dpOrig.getId(), dp.getId() );
					assertEquals( "changed", dp.getDescription() );
					assertEquals( dpOrig.getX(), dp.getX() );
					assertEquals( dpOrig.getY(), dp.getY() );
					session.remove( dp );
				}
		);
	}

	@Test
	public void testReadOnlyInitToModifiableModifiedIsUpdated(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						checkReadOnly( session, dp, false );
						session.setReadOnly( dp, true );
						checkReadOnly( session, dp, true );
						assertFalse( Hibernate.isInitialized( dp ) );
						Hibernate.initialize( dp );
						assertTrue( Hibernate.isInitialized( dp ) );
						checkReadOnly( session, dp, true );
						session.setReadOnly( dp, false );
						checkReadOnly( session, dp, false );
						dp.setDescription( "changed" );
						assertTrue( Hibernate.isInitialized( dp ) );
						assertEquals( "changed", dp.getDescription() );
						session.flush();
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inTransaction(
				session -> {
					DataPoint dp = session.get( DataPoint.class, dpOrig.getId() );
					assertEquals( dpOrig.getId(), dp.getId() );
					assertEquals( "changed", dp.getDescription() );
					assertEquals( dpOrig.getX(), dp.getX() );
					assertEquals( dpOrig.getY(), dp.getY() );
					session.remove( dp );
				}
		);
	}

	@Test
	public void testReadOnlyModifiedMerge(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						checkReadOnly( session, dp, false );
						session.setReadOnly( dp, true );
						checkReadOnly( session, dp, true );
						assertFalse( Hibernate.isInitialized( dp ) );
						dp.setDescription( "changed" );
						assertTrue( Hibernate.isInitialized( dp ) );
						assertEquals( "changed", dp.getDescription() );
						checkReadOnly( session, dp, true );
						session.merge( dp );
						session.flush();
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inTransaction(
				session -> {
					DataPoint dp = session.get( DataPoint.class, dpOrig.getId() );
					assertEquals( dpOrig.getId(), dp.getId() );
					assertEquals( dpOrig.getDescription(), dp.getDescription() );
					assertEquals( dpOrig.getX(), dp.getX() );
					assertEquals( dpOrig.getY(), dp.getY() );
					session.remove( dp );
				}
		);
	}

	@Test
	public void testReadOnlyDelete(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		scope.inSession(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					session.beginTransaction();
					try {
						DataPoint dp = session.getReference( DataPoint.class, dpOrig.getId() );
						assertTrue( dp instanceof HibernateProxy );
						checkReadOnly( session, dp, false );
						session.setReadOnly( dp, true );
						checkReadOnly( session, dp, true );
						assertFalse( Hibernate.isInitialized( dp ) );
						session.remove( dp );
						session.flush();
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inTransaction(
				session -> {
					DataPoint dp = session.get( DataPoint.class, dpOrig.getId() );
					assertNull( dp );
				}
		);
	}

	@Test
	public void testReadOnlyRefresh(SessionFactoryScope scope) {
		Session s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setDescription( "original" );
		dp.setX( new BigDecimal( 0.1d ).setScale( 19, BigDecimal.ROUND_DOWN ) );
		dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale( 19, BigDecimal.ROUND_DOWN ) );
		s.persist( dp );
		t.commit();
		s.close();

		s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );
		t = s.beginTransaction();
		dp = s.getReference( DataPoint.class, dp.getId() );
		s.setReadOnly( dp, true );
		assertFalse( Hibernate.isInitialized( dp ) );
		s.refresh( dp );
		assertTrue( Hibernate.isInitialized( dp ) );
		assertEquals( "original", dp.getDescription() );
		assertTrue( Hibernate.isInitialized( dp ) );
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		assertTrue( s.isReadOnly( dp ) );
		assertTrue( s.isReadOnly( ( (HibernateProxy) dp ).getHibernateLazyInitializer().getImplementation() ) );
		s.refresh( dp );
		assertEquals( "original", dp.getDescription() );
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		assertTrue( s.isReadOnly( dp ) );
		assertTrue( s.isReadOnly( ( (HibernateProxy) dp ).getHibernateLazyInitializer().getImplementation() ) );
		t.commit();

		s.clear();
		t = s.beginTransaction();
		dp = s.get( DataPoint.class, dp.getId() );
		assertEquals( "original", dp.getDescription() );
		s.remove( dp );
		t.commit();
		s.close();
	}

	@Test
	public void testReadOnlyRefreshDeleted(SessionFactoryScope scope) {
		Session s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setDescription( "original" );
		dp.setX( new BigDecimal( 0.1d ).setScale( 19, BigDecimal.ROUND_DOWN ) );
		dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale( 19, BigDecimal.ROUND_DOWN ) );
		s.persist( dp );
		t.commit();
		s.close();

		s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );
		t = s.beginTransaction();
		HibernateProxy dpProxy = (HibernateProxy) s.getReference( DataPoint.class, dp.getId() );
		assertFalse( Hibernate.isInitialized( dpProxy ) );
		t.commit();
		s.close();

		s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );
		t = s.beginTransaction();
		dp = s.get( DataPoint.class, dp.getId() );
		s.remove( dp );
		s.flush();
		try {
			s.refresh( dp );
			fail( "should have thrown IllegalArgumentException" );
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
		finally {
			t.rollback();
			s.close();
		}

		s = openSession( scope );
		t = s.beginTransaction();
		s.setCacheMode( CacheMode.IGNORE );
		DataPoint dpProxyInit = s.getReference( DataPoint.class, dp.getId() );
		assertEquals( "original", dp.getDescription() );
		assertEquals( "original", dpProxyInit.getDescription() );
		s.remove( dpProxyInit );
		t.commit();
		s.close();

		s = openSession( scope );
		t = s.beginTransaction();
		assertTrue( dpProxyInit instanceof HibernateProxy );
		assertTrue( Hibernate.isInitialized( dpProxyInit ) );
		try {
			s.refresh( dpProxyInit );
			fail( "should have thrown IllegalArgumentException" );
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
		finally {
			t.rollback();
			s.close();
		}

		s = openSession( scope );
		t = s.beginTransaction();
		assertTrue( dpProxy instanceof HibernateProxy );
		try {
			s.refresh( dpProxy );
			assertFalse( Hibernate.isInitialized( dpProxy ) );
			Hibernate.initialize( dpProxy );
			fail( "should have thrown IllegalArgumentException" );
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
		finally {
			t.rollback();
			s.close();
		}
	}

	@Test
	public void testReadOnlyProxyMergeDetachedProxyWithChange(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		Session s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );
		s.beginTransaction();
		DataPoint dp = s.getReference( DataPoint.class, dpOrig.getId() );
		assertTrue( dp instanceof HibernateProxy );
		checkReadOnly( s, dp, false );
		assertFalse( Hibernate.isInitialized( dp ) );
		Hibernate.initialize( dp );
		assertTrue( Hibernate.isInitialized( dp ) );
		s.getTransaction().commit();
		s.close();

		// modify detached proxy
		dp.setDescription( "changed" );

		s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );
		s.beginTransaction();
		DataPoint dpLoaded = s.getReference( DataPoint.class, dpOrig.getId() );
		assertTrue( dpLoaded instanceof HibernateProxy );
		checkReadOnly( s, dpLoaded, false );
		s.setReadOnly( dpLoaded, true );
		checkReadOnly( s, dpLoaded, true );
		assertFalse( Hibernate.isInitialized( dpLoaded ) );
		DataPoint dpMerged = s.merge( dp );
		assertSame( dpLoaded, dpMerged );
		assertTrue( Hibernate.isInitialized( dpLoaded ) );
		assertEquals( "changed", dpLoaded.getDescription() );
		checkReadOnly( s, dpLoaded, true );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession( scope );
		s.beginTransaction();
		dp = s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.remove( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testReadOnlyProxyInitMergeDetachedProxyWithChange(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		Session s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );
		s.beginTransaction();
		DataPoint dp = s.getReference( DataPoint.class, dpOrig.getId() );
		assertTrue( dp instanceof HibernateProxy );
		checkReadOnly( s, dp, false );
		assertFalse( Hibernate.isInitialized( dp ) );
		Hibernate.initialize( dp );
		assertTrue( Hibernate.isInitialized( dp ) );
		s.getTransaction().commit();
		s.close();

		// modify detached proxy
		dp.setDescription( "changed" );

		s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );
		s.beginTransaction();
		DataPoint dpLoaded = s.getReference( DataPoint.class, dpOrig.getId() );
		assertTrue( dpLoaded instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dpLoaded ) );
		Hibernate.initialize( dpLoaded );
		assertTrue( Hibernate.isInitialized( dpLoaded ) );
		checkReadOnly( s, dpLoaded, false );
		s.setReadOnly( dpLoaded, true );
		checkReadOnly( s, dpLoaded, true );
		DataPoint dpMerged = s.merge( dp );
		assertSame( dpLoaded, dpMerged );
		assertEquals( "changed", dpLoaded.getDescription() );
		checkReadOnly( s, dpLoaded, true );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession( scope );
		s.beginTransaction();
		dp = s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.remove( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testReadOnlyProxyMergeDetachedEntityWithChange(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		Session s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );
		s.beginTransaction();
		DataPoint dp = s.getReference( DataPoint.class, dpOrig.getId() );
		assertTrue( dp instanceof HibernateProxy );
		checkReadOnly( s, dp, false );
		assertFalse( Hibernate.isInitialized( dp ) );
		Hibernate.initialize( dp );
		assertTrue( Hibernate.isInitialized( dp ) );
		s.getTransaction().commit();
		s.close();

		// modify detached proxy target
		DataPoint dpEntity = (DataPoint) ( (HibernateProxy) dp ).getHibernateLazyInitializer().getImplementation();
		dpEntity.setDescription( "changed" );

		s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );
		s.beginTransaction();
		DataPoint dpLoaded = s.getReference( DataPoint.class, dpOrig.getId() );
		assertTrue( dpLoaded instanceof HibernateProxy );
		checkReadOnly( s, dpLoaded, false );
		s.setReadOnly( dpLoaded, true );
		checkReadOnly( s, dpLoaded, true );
		assertFalse( Hibernate.isInitialized( dpLoaded ) );
		DataPoint dpMerged = s.merge( dpEntity );
		assertSame( dpLoaded, dpMerged );
		assertTrue( Hibernate.isInitialized( dpLoaded ) );
		assertEquals( "changed", dpLoaded.getDescription() );
		checkReadOnly( s, dpLoaded, true );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession( scope );
		s.beginTransaction();
		dp = s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.remove( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testReadOnlyProxyInitMergeDetachedEntityWithChange(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		Session s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );
		s.beginTransaction();
		DataPoint dp = s.getReference( DataPoint.class, dpOrig.getId() );
		assertTrue( dp instanceof HibernateProxy );
		checkReadOnly( s, dp, false );
		assertFalse( Hibernate.isInitialized( dp ) );
		Hibernate.initialize( dp );
		assertTrue( Hibernate.isInitialized( dp ) );
		s.getTransaction().commit();
		s.close();

		// modify detached proxy target
		DataPoint dpEntity = (DataPoint) ( (HibernateProxy) dp ).getHibernateLazyInitializer().getImplementation();
		dpEntity.setDescription( "changed" );

		s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );
		s.beginTransaction();
		DataPoint dpLoaded = s.getReference( DataPoint.class, dpOrig.getId() );
		assertTrue( dpLoaded instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dpLoaded ) );
		Hibernate.initialize( dpLoaded );
		assertTrue( Hibernate.isInitialized( dpLoaded ) );
		checkReadOnly( s, dpLoaded, false );
		s.setReadOnly( dpLoaded, true );
		checkReadOnly( s, dpLoaded, true );
		DataPoint dpMerged = s.merge( dpEntity );
		assertSame( dpLoaded, dpMerged );
		assertEquals( "changed", dpLoaded.getDescription() );
		checkReadOnly( s, dpLoaded, true );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession( scope );
		s.beginTransaction();
		dp = s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.remove( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testReadOnlyEntityMergeDetachedProxyWithChange(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		Session s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );
		s.beginTransaction();
		DataPoint dp = s.getReference( DataPoint.class, dpOrig.getId() );
		assertTrue( dp instanceof HibernateProxy );
		checkReadOnly( s, dp, false );
		assertFalse( Hibernate.isInitialized( dp ) );
		Hibernate.initialize( dp );
		assertTrue( Hibernate.isInitialized( dp ) );
		s.getTransaction().commit();
		s.close();

		// modify detached proxy
		dp.setDescription( "changed" );

		s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );
		s.beginTransaction();
		DataPoint dpEntity = s.get( DataPoint.class, dpOrig.getId() );
		assertFalse( dpEntity instanceof HibernateProxy );
		assertFalse( s.isReadOnly( dpEntity ) );
		s.setReadOnly( dpEntity, true );
		assertTrue( s.isReadOnly( dpEntity ) );
		DataPoint dpMerged = s.merge( dp );
		assertSame( dpEntity, dpMerged );
		assertEquals( "changed", dpEntity.getDescription() );
		assertTrue( s.isReadOnly( dpEntity ) );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession( scope );
		s.beginTransaction();
		dp = s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.remove( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testSetReadOnlyInTwoTransactionsSameSession(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		Session s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );

		s.beginTransaction();
		DataPoint dp = s.getReference( DataPoint.class, dpOrig.getId() );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		s.setReadOnly( dp, true );
		checkReadOnly( s, dp, true );
		assertFalse( Hibernate.isInitialized( dp ) );
		dp.setDescription( "changed" );
		assertTrue( Hibernate.isInitialized( dp ) );
		assertEquals( "changed", dp.getDescription() );
		s.flush();
		s.getTransaction().commit();

		checkReadOnly( s, dp, true );

		s.beginTransaction();
		checkReadOnly( s, dp, true );
		dp.setDescription( "changed again" );
		assertEquals( "changed again", dp.getDescription() );
		s.flush();
		s.getTransaction().commit();

		s.close();

		s = openSession( scope );
		s.beginTransaction();
		dp = s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.remove( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testSetReadOnlyBetweenTwoTransactionsSameSession(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		Session s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );

		s.beginTransaction();
		DataPoint dp = s.getReference( DataPoint.class, dpOrig.getId() );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		dp.setDescription( "changed" );
		assertTrue( Hibernate.isInitialized( dp ) );
		assertEquals( "changed", dp.getDescription() );
		checkReadOnly( s, dp, false );
		s.flush();
		s.getTransaction().commit();

		checkReadOnly( s, dp, false );
		s.setReadOnly( dp, true );
		checkReadOnly( s, dp, true );

		s.beginTransaction();
		checkReadOnly( s, dp, true );
		dp.setDescription( "changed again" );
		assertEquals( "changed again", dp.getDescription() );
		s.flush();
		s.getTransaction().commit();

		s.close();

		s = openSession( scope );
		s.beginTransaction();
		dp = s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( "changed", dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.remove( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testSetModifiableBetweenTwoTransactionsSameSession(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		Session s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );

		s.beginTransaction();
		DataPoint dp = s.getReference( DataPoint.class, dpOrig.getId() );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		s.setReadOnly( dp, true );
		checkReadOnly( s, dp, true );
		dp.setDescription( "changed" );
		assertTrue( Hibernate.isInitialized( dp ) );
		assertEquals( "changed", dp.getDescription() );
		checkReadOnly( s, dp, true );
		s.flush();
		s.getTransaction().commit();

		checkReadOnly( s, dp, true );
		s.setReadOnly( dp, false );
		checkReadOnly( s, dp, false );

		s.beginTransaction();
		checkReadOnly( s, dp, false );
		assertEquals( "changed", dp.getDescription() );
		s.refresh( dp );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		checkReadOnly( s, dp, false );
		dp.setDescription( "changed again" );
		assertEquals( "changed again", dp.getDescription() );
		s.flush();
		s.getTransaction().commit();

		s.close();

		s = openSession( scope );
		s.beginTransaction();
		dp = s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( "changed again", dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.remove( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testIsReadOnlyAfterSessionClosed(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		Session s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );

		s.beginTransaction();
		DataPoint dp = s.getReference( DataPoint.class, dpOrig.getId() );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		s.getTransaction().commit();
		s.close();

		try {
			s.isReadOnly( dp );
			fail( "should have failed because session was closed" );
		}
		catch (IllegalStateException ex) {
			// expected
			assertFalse( ( (HibernateProxy) dp ).getHibernateLazyInitializer().isReadOnlySettingAvailable() );
		}
		finally {
			s = openSession( scope );
			s.beginTransaction();
			s.remove( dp );
			s.getTransaction().commit();
			s.close();
		}
	}

	@Test
	public void testIsReadOnlyAfterSessionClosedViaLazyInitializer(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		Session s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );

		s.beginTransaction();
		DataPoint dp = s.getReference( DataPoint.class, dpOrig.getId() );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		s.getTransaction().commit();
		assertTrue( s.contains( dp ) );
		s.close();

		assertNull( ( (HibernateProxy) dp ).getHibernateLazyInitializer().getSession() );
		try {
			( (HibernateProxy) dp ).getHibernateLazyInitializer().isReadOnly();
			fail( "should have failed because session was detached" );
		}
		catch (IllegalStateException ex) {
			// expected
			assertFalse( ( (HibernateProxy) dp ).getHibernateLazyInitializer().isReadOnlySettingAvailable() );
		}
		finally {
			s = openSession( scope );
			s.beginTransaction();
			s.remove( dp );
			s.getTransaction().commit();
			s.close();
		}
	}

	@Test
	public void testDetachedIsReadOnlyAfterEvictViaSession(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		Session s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );

		s.beginTransaction();
		DataPoint dp = s.getReference( DataPoint.class, dpOrig.getId() );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		assertTrue( s.contains( dp ) );
		s.evict( dp );
		assertFalse( s.contains( dp ) );
		assertNull( ( (HibernateProxy) dp ).getHibernateLazyInitializer().getSession() );

		try {
			s.isReadOnly( dp );
			fail( "should have failed because proxy was detached" );
		}
		catch (IllegalStateException ex) {
			// expected
			assertFalse( ( (HibernateProxy) dp ).getHibernateLazyInitializer().isReadOnlySettingAvailable() );
		}
		finally {
			s.remove( dp );
			s.getTransaction().commit();
			s.close();
		}
	}

	@Test
	public void testDetachedIsReadOnlyAfterEvictViaLazyInitializer(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		Session s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );

		s.beginTransaction();
		DataPoint dp = s.getReference( DataPoint.class, dpOrig.getId() );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		s.evict( dp );
		assertFalse( s.contains( dp ) );
		assertNull( ( (HibernateProxy) dp ).getHibernateLazyInitializer().getSession() );
		try {
			( (HibernateProxy) dp ).getHibernateLazyInitializer().isReadOnly();
			fail( "should have failed because proxy was detached" );
		}
		catch (IllegalStateException ex) {
			// expected
			assertFalse( ( (HibernateProxy) dp ).getHibernateLazyInitializer().isReadOnlySettingAvailable() );
		}
		finally {
			s.remove( dp );
			s.getTransaction().commit();
			s.close();
		}
	}

	@Test
	public void testSetReadOnlyAfterSessionClosed(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		Session s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );

		s.beginTransaction();
		DataPoint dp = s.getReference( DataPoint.class, dpOrig.getId() );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		s.getTransaction().commit();
		s.close();

		try {
			s.setReadOnly( dp, true );
			fail( "should have failed because session was closed" );
		}
		catch (IllegalStateException ex) {
			// expected
			assertFalse( ( (HibernateProxy) dp ).getHibernateLazyInitializer().isReadOnlySettingAvailable() );
		}
		finally {
			s = openSession( scope );
			s.beginTransaction();
			s.remove( dp );
			s.getTransaction().commit();
			s.close();
		}
	}

	@Test
	public void testSetReadOnlyAfterSessionClosedViaLazyInitializer(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		Session s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );

		s.beginTransaction();
		DataPoint dp = s.getReference( DataPoint.class, dpOrig.getId() );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		s.getTransaction().commit();
		assertTrue( s.contains( dp ) );
		s.close();

		assertNull( ( (HibernateProxy) dp ).getHibernateLazyInitializer().getSession() );
		try {
			( (HibernateProxy) dp ).getHibernateLazyInitializer().setReadOnly( true );
			fail( "should have failed because session was detached" );
		}
		catch (IllegalStateException ex) {
			// expected
			assertFalse( ( (HibernateProxy) dp ).getHibernateLazyInitializer().isReadOnlySettingAvailable() );
		}
		finally {
			s = openSession( scope );
			s.beginTransaction();
			s.remove( dp );
			s.getTransaction().commit();
			s.close();
		}
	}

	@Test
	public void testSetClosedSessionInLazyInitializer(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		Session s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );

		s.beginTransaction();
		DataPoint dp = s.getReference( DataPoint.class, dpOrig.getId() );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		s.getTransaction().commit();
		assertTrue( s.contains( dp ) );
		s.close();

		assertNull( ( (HibernateProxy) dp ).getHibernateLazyInitializer().getSession() );
		assertTrue( ( (SessionImplementor) s ).isClosed() );
		try {
			( (HibernateProxy) dp ).getHibernateLazyInitializer().setSession( (SessionImplementor) s );
			fail( "should have failed because session was closed" );
		}
		catch (IllegalStateException ex) {
			// expected
			assertFalse( ( (HibernateProxy) dp ).getHibernateLazyInitializer().isReadOnlySettingAvailable() );
		}
		finally {
			s = openSession( scope );
			s.beginTransaction();
			s.remove( dp );
			s.getTransaction().commit();
			s.close();
		}
	}

	@Test
	public void testDetachedSetReadOnlyAfterEvictViaSession(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		Session s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );

		s.beginTransaction();
		DataPoint dp = s.getReference( DataPoint.class, dpOrig.getId() );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		assertTrue( s.contains( dp ) );
		s.evict( dp );
		assertFalse( s.contains( dp ) );
		assertNull( ( (HibernateProxy) dp ).getHibernateLazyInitializer().getSession() );

		try {
			s.setReadOnly( dp, true );
			fail( "should have failed because proxy was detached" );
		}
		catch (IllegalStateException ex) {
			// expected
			assertFalse( ( (HibernateProxy) dp ).getHibernateLazyInitializer().isReadOnlySettingAvailable() );
		}
		finally {
			s.remove( dp );
			s.getTransaction().commit();
			s.close();
		}
	}

	@Test
	public void testDetachedSetReadOnlyAfterEvictViaLazyInitializer(SessionFactoryScope scope) {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE, scope );

		Session s = openSession( scope );
		s.setCacheMode( CacheMode.IGNORE );

		s.beginTransaction();
		DataPoint dp = s.getReference( DataPoint.class, dpOrig.getId() );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		s.evict( dp );
		assertFalse( s.contains( dp ) );
		assertNull( ( (HibernateProxy) dp ).getHibernateLazyInitializer().getSession() );
		try {
			( (HibernateProxy) dp ).getHibernateLazyInitializer().setReadOnly( true );
			fail( "should have failed because proxy was detached" );
		}
		catch (IllegalStateException ex) {
			// expected
			assertFalse( ( (HibernateProxy) dp ).getHibernateLazyInitializer().isReadOnlySettingAvailable() );
		}
		finally {
			s.remove( dp );
			s.getTransaction().commit();
			s.close();
		}
	}

	private Session openSession(SessionFactoryScope scope) {
		return scope.getSessionFactory().openSession();
	}

	private DataPoint createDataPoint(CacheMode cacheMode, SessionFactoryScope scope) {
		Session s = openSession( scope );
		s.setCacheMode( cacheMode );
		s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setX( new BigDecimal( 0.1d ).setScale( 19, BigDecimal.ROUND_DOWN ) );
		dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale( 19, BigDecimal.ROUND_DOWN ) );
		dp.setDescription( "original" );
		s.persist( dp );
		s.getTransaction().commit();
		s.close();
		return dp;
	}

	private void checkReadOnly(Session s, Object proxy, boolean expectedReadOnly) {
		assertTrue( proxy instanceof HibernateProxy );
		LazyInitializer li = ( (HibernateProxy) proxy ).getHibernateLazyInitializer();
		assertSame( s, li.getSession() );
		assertEquals( expectedReadOnly, s.isReadOnly( proxy ) );
		assertEquals( expectedReadOnly, li.isReadOnly() );
		assertEquals( Hibernate.isInitialized( proxy ), !li.isUninitialized() );
		if ( Hibernate.isInitialized( proxy ) ) {
			assertEquals( expectedReadOnly, s.isReadOnly( li.getImplementation() ) );
		}
	}
}
