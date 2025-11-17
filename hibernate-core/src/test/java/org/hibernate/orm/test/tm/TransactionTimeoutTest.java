/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tm;

import jakarta.persistence.PersistenceException;

import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.jdbc.Person;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Lukasz Antoniak
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-6780")
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.IMPLICIT_NAMING_STRATEGY, value = "legacy-hbm" )
		}
)
@DomainModel(
		xmlMappings = {"org/hibernate/orm/test/jdbc/Mappings.hbm.xml"}
)
@SessionFactory
public class TransactionTimeoutTest {

	@Test
	public void testJdbcCoordinatorTransactionTimeoutCheck(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					try {
						Transaction transaction = session.getTransaction();
						transaction.setTimeout( 2 );
						assertEquals( -1, session.getJdbcCoordinator().determineRemainingTransactionTimeOutPeriod() );
						transaction.begin();
						assertNotSame( -1, session.getJdbcCoordinator().determineRemainingTransactionTimeOutPeriod() );
						transaction.commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}

	@Test
	public void testTransactionTimeoutFailure(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					try {
						Transaction transaction = session.getTransaction();
						transaction.setTimeout( 1 );
						assertEquals( -1, session.getJdbcCoordinator().determineRemainingTransactionTimeOutPeriod() );
						transaction.begin();
						Thread.sleep( 1000 );
						session.persist( new Person( "Lukasz", "Antoniak" ) );
						transaction.commit();
					}
					catch (TransactionException e) {
						// expected
					}
					catch (PersistenceException e) {
						assertTyping( TransactionException.class, e.getCause() );
					}
					catch (InterruptedException ie) {
						fail(ie.getCause());
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);
	}

	@Test
	public void testTransactionTimeoutSuccess(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					try {
						Transaction transaction = session.getTransaction();
						transaction.setTimeout( 60 );
						transaction.begin();
						session.persist( new Person( "Lukasz", "Antoniak" ) );
						transaction.commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}
}
