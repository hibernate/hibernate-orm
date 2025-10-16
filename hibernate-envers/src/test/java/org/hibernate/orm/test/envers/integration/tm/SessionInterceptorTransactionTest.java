/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.tm;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.envers.entities.StrTestEntity;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.AfterClassTemplate;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingConfiguration;
import org.junit.jupiter.api.Test;

import jakarta.transaction.TransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Chris Cranford
 */
@EnversTest
@Jpa(
		annotatedClasses = { StrTestEntity.class },
		integrationSettings = {
				@Setting(name = AvailableSettings.SESSION_SCOPED_INTERCEPTOR, value = "org.hibernate.orm.test.envers.integration.tm.TestInterceptor"),
				@Setting(name = AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS, value = "true"),
		},
		settingConfigurations = { @SettingConfiguration(configurer = TestingJtaBootstrap.class) }
)
public class SessionInterceptorTransactionTest {

	private TransactionManager tm;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) throws Exception {
		tm = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
		// ensure schema is created before manually starting transactions
		final var emf = scope.getEntityManagerFactory();

		// Revision 1
		final var em = emf.createEntityManager();
		// Explicitly use manual flush to trigger separate temporary session write via Envers
		em.unwrap( Session.class ).setHibernateFlushMode( FlushMode.MANUAL );
		tm.begin();
		StrTestEntity entity = new StrTestEntity( "Test" );
		em.persist( entity );
		em.flush();
		tm.commit();
	}

	@AfterClassTemplate
	public void cleanUp() {
		TestInterceptor.reset();
	}

	@Test
	public void testInterceptorInvocations() {
		// The interceptor should be created one and should only be invoked once
		final var invocationMap = TestInterceptor.getBeforeCompletionCallbacks();
		assertEquals( 1, invocationMap.size() );
		invocationMap.values().forEach( c -> assertEquals( 1, c.intValue() ) );
	}
}
