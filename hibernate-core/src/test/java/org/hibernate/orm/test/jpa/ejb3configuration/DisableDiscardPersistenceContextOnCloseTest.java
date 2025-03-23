/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ejb3configuration;

import java.sql.SQLException;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.jpa.Wallet;

import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProviderSettingProvider;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJdbcDriverProxying.class)
@Jpa(
		annotatedClasses = { Wallet.class },
		integrationSettings = { @Setting( name = AvailableSettings.DISCARD_PC_ON_CLOSE, value = "false") },
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.CONNECTION_PROVIDER,
						provider = PreparedStatementSpyConnectionProviderSettingProvider.class)
		}
)
public class DisableDiscardPersistenceContextOnCloseTest {

	private PreparedStatementSpyConnectionProvider connectionProvider;

	@BeforeAll
	public void init(EntityManagerFactoryScope scope) {
		Map props = scope.getEntityManagerFactory().getProperties();
		connectionProvider = (PreparedStatementSpyConnectionProvider) props.get( AvailableSettings.CONNECTION_PROVIDER );
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		// If the line below is uncommented a "Database is already closed" stack trace appears in the log
//		connectionProvider.stop();
	}

	@Test
	public void testDiscardOnClose(EntityManagerFactoryScope scope) throws SQLException {
		scope.inEntityManager(
				entityManager -> {
					Wallet wallet = new Wallet();
					wallet.setSerial( "123" );

					try {
						entityManager.getTransaction().begin();
						// Force connection acquisition
						entityManager.createQuery( "select 1" ).getResultList();
						entityManager.persist( wallet );
						assertEquals( 1, connectionProvider.getAcquiredConnections().size() );
						entityManager.close();
						assertEquals( 1, connectionProvider.getAcquiredConnections().size() );
						assertTrue( entityManager.getTransaction().isActive() );
						assertEquals( 1, connectionProvider.getAcquiredConnections().size() );
						entityManager.getTransaction().rollback();
						assertEquals( 0, connectionProvider.getAcquiredConnections().size() );
						assertFalse( entityManager.getTransaction().isActive() );
					}
					catch (Exception e) {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}
}
