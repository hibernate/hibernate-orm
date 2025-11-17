/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.resource.transaction.jta;

import jakarta.transaction.TransactionManager;
import org.hibernate.Transaction;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.JpaComplianceSettings.JPA_TRANSACTION_COMPLIANCE;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Same as {@linkplain NonJpaComplianceAlreadyStartedTransactionTest}, but here with JPA compliance enabled
 *
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-13076")
@ServiceRegistry(
		settingConfigurations = @SettingConfiguration( configurer = TestingJtaBootstrap.class ),
		settings = {
				@Setting( name = JPA_TRANSACTION_COMPLIANCE, value = "true" )
		}
)
@DomainModel
@SessionFactory
@SuppressWarnings("JUnitMalformedDeclaration")
public class JpaComplianceAlreadyStartedTransactionTest {
	private TransactionManager tm;

	@BeforeEach
	public void setUp() {
		tm = JtaPlatformStandardTestingImpl.INSTANCE.transactionManager();
	}

	@Test
	public void testBeginWithinActiveTransaction(SessionFactoryScope sessions) throws Exception {
		TestingJtaPlatformImpl.inNoopJtaTransaction( tm, () -> {
			sessions.inSession( (session) -> {
				Transaction tx = null;
				try {
					// A call to begin() with an active Tx should cause an IllegalStateException
					tx = session.getTransaction();
					tx.begin();
					fail( "Expecting an IllegalStateException" );
				}
				catch (IllegalStateException expected) {
				}
				finally {
					if ( tx != null ) {
						try {
							tx.rollback();
						}
						catch (Exception ignore) {
						}
					}
				}
			} );
		} );
	}

}
