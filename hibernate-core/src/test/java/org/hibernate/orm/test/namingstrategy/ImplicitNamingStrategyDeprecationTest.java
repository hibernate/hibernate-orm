/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import java.util.stream.Stream;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyHbmImpl;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.boot.model.naming.spi.StandardImplicitNamingStrategy;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.Logger;
import org.hibernate.testing.orm.junit.MessageKeyInspection;
import org.hibernate.testing.orm.junit.MessageKeyWatcher;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Warn for the effective deprecated strategy, regardless of how it was supplied.
///
/// @author Steve Ebersole
@BaseUnitTest
@MessageKeyInspection(messageKey = "HHH90000046", logger = @Logger(loggerName = DeprecationLogger.CATEGORY))
class ImplicitNamingStrategyDeprecationTest {
	static Stream<Object> configuredStrategies() {
		return Stream.of(
				"legacy-hbm", "legacy-jpa", "component-path",
				ImplicitNamingStrategyLegacyHbmImpl.class.getName(),
				ImplicitNamingStrategyLegacyJpaImpl.class.getName(),
				ImplicitNamingStrategyComponentPathImpl.class.getName(),
				ImplicitNamingStrategyLegacyHbmImpl.class,
				ImplicitNamingStrategyLegacyJpaImpl.class,
				ImplicitNamingStrategyComponentPathImpl.class,
				new ImplicitNamingStrategyLegacyHbmImpl(),
				new ImplicitNamingStrategyLegacyJpaImpl(),
				new ImplicitNamingStrategyComponentPathImpl(),
				new CustomLegacyStrategy() );
	}

	@ParameterizedTest @MethodSource("configuredStrategies")
	void configuredStrategyWarnsOnce(Object strategy, MessageKeyWatcher watcher) {
		watcher.reset();
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.IMPLICIT_NAMING_STRATEGY, strategy ).build()) {
			MetadataBuildingTestHelper.buildMetadata( registry, new MappingSources() );
			assertThat( watcher.getTriggeredMessages() ).hasSize( 1 );
		}
	}

	static Stream<ImplicitNamingStrategy> suppliedStrategies() {
		return Stream.of( new ImplicitNamingStrategyLegacyHbmImpl(), new ImplicitNamingStrategyLegacyJpaImpl(),
				new ImplicitNamingStrategyComponentPathImpl(), new CustomLegacyStrategy() );
	}

	@ParameterizedTest @MethodSource("suppliedStrategies")
	void suppliedStrategyWarnsOnce(ImplicitNamingStrategy strategy, MessageKeyWatcher watcher) {
		watcher.reset();
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			MetadataBuildingTestHelper.buildMetadataWithImplicitNaming( registry, new MappingSources(), strategy );
			assertThat( watcher.getTriggeredMessages() ).singleElement()
					.asString().contains( strategy.getClass().getName() );
		}
	}

	@Test
	void supportedStrategiesDoNotWarnEvenWhenOverridingDeprecatedConfiguration(MessageKeyWatcher watcher) {
		for ( var strategy : new ImplicitNamingStrategy[] {new StandardImplicitNamingStrategy(), new ImplicitNamingStrategyJpaCompliantImpl()} ) {
			watcher.reset();
			try (var registry = ServiceRegistryUtil.serviceRegistryBuilder()
					.applySetting( AvailableSettings.IMPLICIT_NAMING_STRATEGY, "legacy-hbm" ).build()) {
				MetadataBuildingTestHelper.buildMetadataWithImplicitNaming( registry, new MappingSources(), strategy );
				assertThat( watcher.wasTriggered() ).isFalse();
			}
		}
	}

	public static class CustomLegacyStrategy extends ImplicitNamingStrategyLegacyHbmImpl {
	}
}
