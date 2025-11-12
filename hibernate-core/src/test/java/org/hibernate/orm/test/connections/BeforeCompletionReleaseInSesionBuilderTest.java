/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.connections;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.jpa.transaction.JtaPlatformSettingProvider;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;

import static org.hibernate.cfg.TransactionSettings.TRANSACTION_COORDINATOR_STRATEGY;

/**
 * @author Luis Barreiro
 */
@ServiceRegistry(
		settings = @Setting(name = TRANSACTION_COORDINATOR_STRATEGY, value = "jta"),
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.CONNECTION_PROVIDER,
						provider = AbstractBeforeCompletionReleaseTest.ConnectionProviderSettingProvider.class
				),
				@SettingProvider(
						settingName = AvailableSettings.CONNECTION_HANDLING,
						provider = BeforeCompletionReleaseInSesionBuilderTest.PhysicalConnectionHandlingModeSettingProvider.class
				),
				@SettingProvider(settingName = AvailableSettings.JTA_PLATFORM,
						provider = JtaPlatformSettingProvider.class),
		}
)
public class BeforeCompletionReleaseInSesionBuilderTest extends AbstractBeforeCompletionReleaseTest{


	public static class PhysicalConnectionHandlingModeSettingProvider
			implements SettingProvider.Provider<PhysicalConnectionHandlingMode> {
		@Override
		public PhysicalConnectionHandlingMode getSetting() {
			return PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT;
		}
	}

	public PhysicalConnectionHandlingMode getConnectionHandlingModeInSessionBuilder() {
		return PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_BEFORE_TRANSACTION_COMPLETION;
	}

}
