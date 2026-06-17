/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.actionqueue;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionManager;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.jpa.transaction.JtaPlatformSettingProvider;

import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Logger;
import org.hibernate.testing.orm.junit.MessageKeyInspection;
import org.hibernate.testing.orm.junit.MessageKeyWatcher;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * In a container-managed JTA context the session is closed from within an <em>interposed</em>
 * {@link Synchronization#afterCompletion(int)} (e.g. WildFly's
 * {@code TransactionUtil$SessionSynchronization}). Per the JTA contract interposed
 * {@code afterCompletion} callbacks run before regular ones, while Hibernate registers its own
 * callback-processing synchronization as a regular one. So the session is closed while the
 * transaction is no longer active but before Hibernate has processed the bulk-operation cleanup
 * callbacks; {@code SessionImpl}'s close guard then force-executes them and logs HHH90010101 /
 * HHH90010108, even though the (regular) synchronization processes them moments later.
 *
 * @see <a href="https://hibernate.atlassian.net/browse/HHH-20583">HHH-20583</a>
 */
@Jpa(
		annotatedClasses = JtaSessionCloseBulkCleanupTest.Author.class,
		settingProviders = @SettingProvider(
				settingName = AvailableSettings.JTA_PLATFORM,
				provider = JtaPlatformSettingProvider.class
		),
		integrationSettings = {
				@Setting(name = AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, value = "jta"),
				@Setting(name = AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS, value = "true"),
				@Setting(name = AvailableSettings.JPA_TRANSACTION_COMPLIANCE, value = "true"),
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(
						name = AvailableSettings.CACHE_REGION_FACTORY,
						value = "org.hibernate.testing.cache.CachingRegionFactory"
				)
		}
)
@MessageKeyInspection(
		messageKey = "HHH90010108",
		logger = @Logger(loggerName = "org.hibernate.orm.session")
)
public class JtaSessionCloseBulkCleanupTest {

	@Test
	void bulkNativeUpdateClosedDuringAfterCompletionDoesNotWarn(
			EntityManagerFactoryScope scope,
			MessageKeyWatcher watcher) throws Exception {

		final SessionFactory sessionFactory = scope.getEntityManagerFactory().unwrap( SessionFactory.class );
		final TransactionManager tm = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();

		tm.begin();
		final Session session = sessionFactory.openSession();

		// No synchronized query spaces -> the bulk cleanup is treated as affecting all cacheable
		// entities, so it is non-empty and registers an after-completion callback.
		session.createNativeMutationQuery( "update AUTHORS set NAME = NAME" ).executeUpdate();

		// Mimic the container: close the session from an interposed afterCompletion (runs before
		// Hibernate's own regular synchronization).
		TestingJtaPlatformImpl.synchronizationRegistry().registerInterposedSynchronization( new Synchronization() {
			@Override
			public void beforeCompletion() {
			}

			@Override
			public void afterCompletion(int status) {
				session.close();
			}
		} );

		tm.commit();

		assertFalse(
				watcher.wasTriggered(),
				"Spurious unprocessed-completion warning(s): " + watcher.getTriggeredMessages()
		);
	}

	@Entity(name = "Author")
	@Table(name = "AUTHORS")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Author {
		@Id
		Long id;
		@Column(name = "NAME")
		String name;
	}
}
