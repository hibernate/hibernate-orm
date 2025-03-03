/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.flush;

import jakarta.persistence.TransactionRequiredException;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Michiel Hendriks
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-13936")
@ServiceRegistry(
		settingConfigurations = @SettingConfiguration( configurer = TestingJtaBootstrap.class )
)
@DomainModel
@SessionFactory
public class TestFlushJoinTransaction {
	@Test
	public void testFlush(SessionFactoryScope sessions) {
		sessions.inSession( (session) -> {
			try {
				TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
				session.flush();
				TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
			}
			catch (TransactionRequiredException e) {
				fail("No TransactionRequiredException expected.");
			}
			catch (Exception e) {
				fail("Unexpected JTA exception", e);
			}
		} );
	}

	@Test
	public void testIsConnectedFlush(SessionFactoryScope sessions) {
		sessions.inSession( (session) -> {
			try {
				TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
				session.isConnected();
				session.flush();
				TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
			}
			catch (TransactionRequiredException e) {
				fail("No TransactionRequiredException expected.");
			}
			catch (Exception e) {
				fail("Unexpected JTA exception", e);
			}
		} );
	}

	@Test
	public void testIsConnectedFlushShouldThrowExceptionIfNoTransaction(SessionFactoryScope sessions) {
		sessions.inSession( (session) -> {
			try {
				session.isConnected();
				session.flush();
				fail("A TransactionRequiredException should be thrown");
			}
			catch (TransactionRequiredException e) {
				//expected
			}
		} );
	}
}
