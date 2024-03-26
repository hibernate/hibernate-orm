/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.ejb3configuration;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.jpa.Wallet;

import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProviderSettingProvider;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJdbcDriverProxying.class)
@Jpa(
		annotatedClasses = { Wallet.class },
		integrationSettings = { @Setting(name = AvailableSettings.DISCARD_PC_ON_CLOSE, value = "true") },
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.CONNECTION_PROVIDER,
						provider = PreparedStatementSpyConnectionProviderSettingProvider.class)
		}
)
public class EnableDiscardPersistenceContextOnCloseTest {

	private PreparedStatementSpyConnectionProvider connectionProvider;

	@BeforeAll
	public void init(EntityManagerFactoryScope scope) {
		Map props = scope.getEntityManagerFactory().getProperties();
		connectionProvider = (PreparedStatementSpyConnectionProvider) props.get( org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER );
	}

	@Test
	public void testDiscardOnClose(EntityManagerFactoryScope scope) {
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
						assertEquals( 0, connectionProvider.getAcquiredConnections().size() );
						assertTrue( entityManager.getTransaction().isActive() );
					}
					finally {
						try {
							entityManager.getTransaction().rollback();
							fail( "Should throw IllegalStateException because the Connection is already closed!" );
						}
						catch (IllegalStateException expected) {
						}
					}

				}
		);
	}
}
