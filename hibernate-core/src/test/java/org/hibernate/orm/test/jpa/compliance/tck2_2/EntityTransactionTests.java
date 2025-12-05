/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance.tck2_2;

import jakarta.persistence.RollbackException;

import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil2.inSession;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry(settings = {@Setting(name = AvailableSettings.JPA_TRANSACTION_COMPLIANCE, value = "true")})
public class EntityTransactionTests {

	@Test
	public void testGetRollbackOnlyExpectations(ServiceRegistryScope scope) {

		try (SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) new MetadataSources(
				scope.getRegistry() )
				.buildMetadata()
				.buildSessionFactory()) {
			inSession(
					sessionFactory,
					session -> {
						final Transaction transaction = session.getTransaction();
						assertFalse( transaction.isActive() );
						try {
							transaction.getRollbackOnly();
							fail( "Expecting failure #getRollbackOnly on non-active txn" );
						}
						catch (IllegalStateException expected) {
						}
					}
			);
		}
	}

	@Test
	public void testMarkRollbackOnlyNoTransaction(ServiceRegistryScope scope) {

		try (SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) new MetadataSources(
				scope.getRegistry() )
				.buildMetadata()
				.buildSessionFactory()) {
			inSession(
					sessionFactory,
					session -> {
						final Transaction transaction = session.getTransaction();
						assertFalse( transaction.isActive() );

						// should just happen silently because there is no transaction
						transaction.markRollbackOnly();

						transaction.begin();
						transaction.commit();
					}
			);
		}
	}

	@Test
	public void testSetRollbackOnlyOutcomeExpectations(ServiceRegistryScope scope) {

		try (SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) new MetadataSources(
				scope.getRegistry() )
				.buildMetadata()
				.buildSessionFactory()) {
			inSession(
					sessionFactory,
					session -> {
						final Transaction transaction = session.getTransaction();
						transaction.begin();

						try {
							assertTrue( transaction.isActive() );

							transaction.setRollbackOnly();
							assertTrue( transaction.isActive() );
							assertTrue( transaction.getRollbackOnly() );
						}
						finally {
							if ( transaction.isActive() ) {
								transaction.rollback();
							}
						}
					}
			);

			inSession(
					sessionFactory,
					session -> {
						final Transaction transaction = session.getTransaction();
						transaction.begin();

						try {
							assertTrue( transaction.isActive() );

							transaction.setRollbackOnly();
							assertTrue( transaction.isActive() );
							assertTrue( transaction.getRollbackOnly() );

							// now try to commit, this should force a rollback
							try {
								transaction.commit();
							}
							catch (RollbackException e) {
								assertFalse( transaction.isActive() );
								assertThat( transaction.getStatus(), CoreMatchers.is( TransactionStatus.ROLLED_BACK ) );
							}
						}
						finally {
							if ( transaction.isActive() ) {
								transaction.rollback();
							}
						}
					}
			);
		}
	}

	@Test
	public void testSetRollbackOnlyExpectations(ServiceRegistryScope scope) {

		try (SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) new MetadataSources(
				scope.getRegistry() )
				.buildMetadata()
				.buildSessionFactory()) {
			inSession(
					sessionFactory,
					session -> {
						final Transaction transaction = session.getTransaction();
						assertFalse( transaction.isActive() );
						try {
							transaction.setRollbackOnly();
							fail( "Expecting failure #setRollbackOnly on non-active txn" );
						}
						catch (IllegalStateException expected) {
						}
					}
			);
		}
	}

	@Test
	public void testRollbackExpectations(ServiceRegistryScope scope) {

		try (SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) new MetadataSources(
				scope.getRegistry() )
				.buildMetadata()
				.buildSessionFactory()) {
			inSession(
					sessionFactory,
					session -> {
						final Transaction transaction = session.getTransaction();
						assertFalse( transaction.isActive() );
						try {
							transaction.rollback();
							fail( "Expecting failure #rollback on non-active txn" );
						}
						catch (IllegalStateException expected) {
						}
					}
			);
		}
	}
}
