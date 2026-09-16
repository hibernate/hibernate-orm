/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.cfg.TransactionSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lock.spi.TransactionConcurrencies;
import org.hibernate.dialect.lock.spi.TransactionConcurrency;
import org.hibernate.testing.orm.junit.DialectFeatureCheck;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.PermitsReadWhileWriteUncommitted;
import org.hibernate.testing.orm.junit.PermitsWriteAfterReadStatement;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.dialect.lock.spi.BlockingDuration.NONE;
import static org.hibernate.dialect.lock.spi.BlockingDuration.STATEMENT;
import static org.hibernate.dialect.lock.spi.BlockingDuration.TRANSACTION;
import static org.hibernate.dialect.lock.spi.Operation.READ;
import static org.hibernate.dialect.lock.spi.Operation.WRITE;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

/// Exercises the real JUnit lifecycle, including factory overrides and setup ordering.
///
/// @since 8.0
/// @author Steve Ebersole
class TransactionConcurrencyFeatureCheckTest {
	private static final List<String> events = new ArrayList<>();

	@Test
	void evaluatesSessionFactoryBeforeUserSetup() {
		verify( SessionFixture.class, 1, 0, 0 );
		assertThat( events ).containsExactly( "descriptor", "setup", "body" );
	}

	@Test
	void evaluatesJpaBeforeUserSetup() {
		verify( JpaFixture.class, 1, 0, 0 );
		assertThat( events ).containsExactly( "descriptor", "setup", "body" );
	}

	@Test
	void usesMethodSessionFactoryInsteadOfClassFactory() {
		verify( MethodSessionFixture.class, 1, 1, 0 );
		assertThat( events ).contains( "body" ).doesNotContain( "wrong body" );
	}

	@Test
	void usesMethodJpaInsteadOfClassFactory() {
		verify( MethodJpaFixture.class, 1, 1, 0 );
		assertThat( events ).contains( "body" ).doesNotContain( "wrong body" );
	}

	@Test
	void reversalDoesNotTurnUnknownIntoMatch() {
		final var messages = verify( UnknownFixture.class, 0, 2, 0 );
		assertThat( events ).doesNotContain( "setup", "body" );
		assertThat( messages ).anyMatch( m -> m.contains( "UNDETERMINED" ) && m.contains( "reverse=true" )
				&& m.contains( "unknown fixture" ) && m.contains( "explain abort" ) );
	}

	@Test
	void reversesEstablishedNonMatch() {
		verify( ReversedFixture.class, 1, 0, 0 );
	}

	@Test
	void missingScopeIsAnError() {
		assertThat( verify( MissingScopeFixture.class, 0, 0, 1 ) )
				.anyMatch( m -> m.contains( "managed factory scope" ) );
	}

	@Test
	void bootstrapFailureIsNotAnAbort() {
		assertThat( verify( BrokenFixture.class, 0, 0, 1 ) )
				.anyMatch( m -> m.contains( "deliberate bootstrap failure" ) );
	}

	@Test
	void ordinaryDialectCheckNeedsNoFactory() {
		verify( DialectOnlyFixture.class, 1, 0, 0 );
		assertThat( events ).containsExactly( "body" );
	}

	private static List<String> verify(Class<?> fixture, long succeeded, long aborted, long failed) {
		events.clear();
		final var summary = new SummaryGeneratingListener();
		final List<String> messages = new ArrayList<>();
		try ( var session = LauncherFactory.openSession() ) {
			session.getLauncher().execute( request().selectors( selectClass( fixture ) )
					.configurationParameter( "hibernate.testing.concurrency.fixture", "true" ).build(), summary,
					new TestExecutionListener() {
						@Override
						public void executionFinished(TestIdentifier identifier, TestExecutionResult result) {
							result.getThrowable().ifPresent( failure -> {
								for ( var cause = failure; cause != null; cause = cause.getCause() ) {
									messages.add( String.valueOf( cause.getMessage() ) );
								}
							} );
						}
					} );
		}
		assertThat( summary.getSummary().getTestsSucceededCount() ).as( messages.toString() ).isEqualTo( succeeded );
		assertThat( summary.getSummary().getTestsAbortedCount() ).as( messages.toString() ).isEqualTo( aborted );
		assertThat( summary.getSummary().getTotalFailureCount() ).as( messages.toString() ).isEqualTo( failed );
		return messages;
	}

	/// Prevents intentional failure fixtures from running during normal suite discovery.
	///
	/// @since 8.0
	/// @author Steve Ebersole
	public static class OnlyLaunchedFixture implements ExecutionCondition {
		@Override
		public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
			return context.getConfigurationParameter( "hibernate.testing.concurrency.fixture" )
					.filter( "true"::equals ).isPresent()
					? ConditionEvaluationResult.enabled( "Launched by lifecycle test" )
					: ConditionEvaluationResult.disabled( "Lifecycle test fixture" );
		}
	}

	/// @since 8.0
	/// @author Steve Ebersole
	public static class Allowed implements SettingProvider.Provider<TransactionConcurrency> {
		@Override
		public TransactionConcurrency getSetting() {
			events.add( "descriptor" );
			return TransactionConcurrencies.builder( "allowed fixture" )
					.blocking( READ, WRITE, STATEMENT ).blocking( WRITE, READ, NONE ).build();
		}
	}

	/// @since 8.0
	/// @author Steve Ebersole
	public static class Unknown implements SettingProvider.Provider<TransactionConcurrency> {
		@Override
		public TransactionConcurrency getSetting() {
			return TransactionConcurrencies.builder( "unknown fixture" ).build();
		}
	}

	/// @since 8.0
	/// @author Steve Ebersole
	public static class Blocking implements SettingProvider.Provider<TransactionConcurrency> {
		@Override
		public TransactionConcurrency getSetting() {
			return TransactionConcurrencies.builder( "blocking fixture" ).blocking( READ, WRITE, TRANSACTION ).build();
		}
	}

	/// @since 8.0
	/// @author Steve Ebersole
	public static class Broken implements SettingProvider.Provider<TransactionConcurrency> {
		@Override
		public TransactionConcurrency getSetting() {
			throw new IllegalStateException( "deliberate bootstrap failure" );
		}
	}

	/// @since 8.0
	/// @author Steve Ebersole
	public static class Always implements DialectFeatureCheck {
		@Override
		public boolean apply(Dialect dialect) {
			return true;
		}
	}

	/// @since 8.0
	/// @author Steve Ebersole
	@DomainModel
	@SessionFactory(exportSchema = false)
	@ServiceRegistry(settingProviders = @SettingProvider(settingName = TransactionSettings.TRANSACTION_CONCURRENCY, provider = Allowed.class))
	@RequiresDialectFeature(feature = PermitsWriteAfterReadStatement.class)
	@RequiresDialectFeature(feature = PermitsReadWhileWriteUncommitted.class)
	@ExtendWith(OnlyLaunchedFixture.class)
	public static class SessionFixture {
		@BeforeEach
		void setup() { events.add( "setup" ); }
		@Test
		void test() { events.add( "body" ); }
	}

	/// @since 8.0
	/// @author Steve Ebersole
	@Jpa(exportSchema = false, settingProviders = @SettingProvider(settingName = TransactionSettings.TRANSACTION_CONCURRENCY, provider = Allowed.class))
	@RequiresDialectFeature(feature = PermitsWriteAfterReadStatement.class)
	@ExtendWith(OnlyLaunchedFixture.class)
	public static class JpaFixture {
		@BeforeEach
		void setup() { events.add( "setup" ); }
		@Test
		void test() { events.add( "body" ); }
	}

	/// @since 8.0
	/// @author Steve Ebersole
	@DomainModel
	@SessionFactory(exportSchema = false)
	@ServiceRegistry(settingProviders = @SettingProvider(settingName = TransactionSettings.TRANSACTION_CONCURRENCY, provider = Unknown.class))
	@RequiresDialectFeature(feature = PermitsWriteAfterReadStatement.class)
	@ExtendWith(OnlyLaunchedFixture.class)
	public static class MethodSessionFixture {
		@Test
		void classFactory() { events.add( "wrong body" ); }
		@Test
		@SessionFactory(exportSchema = false)
		@ServiceRegistry(settingProviders = @SettingProvider(settingName = TransactionSettings.TRANSACTION_CONCURRENCY, provider = Allowed.class))
		void methodFactory() { events.add( "body" ); }
	}

	/// @since 8.0
	/// @author Steve Ebersole
	@Jpa(exportSchema = false, settingProviders = @SettingProvider(settingName = TransactionSettings.TRANSACTION_CONCURRENCY, provider = Unknown.class))
	@RequiresDialectFeature(feature = PermitsWriteAfterReadStatement.class)
	@ExtendWith(OnlyLaunchedFixture.class)
	public static class MethodJpaFixture {
		@Test
		void classFactory() { events.add( "wrong body" ); }
		@Test
		@Jpa(exportSchema = false, settingProviders = @SettingProvider(settingName = TransactionSettings.TRANSACTION_CONCURRENCY, provider = Allowed.class))
		void methodFactory() { events.add( "body" ); }
	}

	/// @since 8.0
	/// @author Steve Ebersole
	@DomainModel
	@SessionFactory(exportSchema = false)
	@ServiceRegistry(settingProviders = @SettingProvider(settingName = TransactionSettings.TRANSACTION_CONCURRENCY, provider = Unknown.class))
	@ExtendWith(OnlyLaunchedFixture.class)
	public static class UnknownFixture {
		@BeforeEach
		void setup() { events.add( "setup" ); }
		@Test
		@RequiresDialectFeature(feature = PermitsWriteAfterReadStatement.class)
		void normal() { events.add( "body" ); }
		@Test
		@RequiresDialectFeature(feature = PermitsWriteAfterReadStatement.class, reverse = true, comment = "explain abort")
		void reversed() { events.add( "body" ); }
	}

	/// @since 8.0
	/// @author Steve Ebersole
	@DomainModel
	@SessionFactory(exportSchema = false)
	@ServiceRegistry(settingProviders = @SettingProvider(settingName = TransactionSettings.TRANSACTION_CONCURRENCY, provider = Blocking.class))
	@RequiresDialectFeature(feature = PermitsWriteAfterReadStatement.class, reverse = true)
	@ExtendWith(OnlyLaunchedFixture.class)
	public static class ReversedFixture {
		@Test
		void test() { events.add( "body" ); }
	}

	/// @since 8.0
	/// @author Steve Ebersole
	@ExtendWith(OnlyLaunchedFixture.class)
	public static class MissingScopeFixture {
		@Test
		@RequiresDialectFeature(feature = PermitsWriteAfterReadStatement.class)
		void test() { events.add( "body" ); }
	}

	/// @since 8.0
	/// @author Steve Ebersole
	@DomainModel
	@SessionFactory(exportSchema = false)
	@ServiceRegistry(settingProviders = @SettingProvider(settingName = TransactionSettings.TRANSACTION_CONCURRENCY, provider = Broken.class))
	@RequiresDialectFeature(feature = PermitsWriteAfterReadStatement.class)
	@ExtendWith(OnlyLaunchedFixture.class)
	public static class BrokenFixture {
		@Test
		void test() { events.add( "body" ); }
	}

	/// @since 8.0
	/// @author Steve Ebersole
	@RequiresDialectFeature(feature = Always.class)
	@ExtendWith(OnlyLaunchedFixture.class)
	public static class DialectOnlyFixture {
		@Test
		void test() { events.add( "body" ); }
	}
}
