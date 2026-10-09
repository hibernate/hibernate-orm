package org.hibernate.orm.test.interceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.hibernate.Interceptor;
import org.hibernate.boot.ConflictingInterceptorSettingsException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.selector.spi.StrategySelectionException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.EmptyInterceptor;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.SessionEventSettings.INTERCEPTOR;
import static org.hibernate.cfg.SessionEventSettings.SESSION_SCOPED_INTERCEPTOR;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Baseline (pre-CDI) cross-check of how {@code SessionEventSettings#INTERCEPTOR} /
 * {@code #SESSION_SCOPED_INTERCEPTOR} and the corresponding {@link SessionFactoryBuilder}
 * methods interact when resolving the {@link Interceptor} used by sessions opened
 * directly from the {@link org.hibernate.SessionFactory}.
 * <p>
 * The two settings are mutually exclusive. Once settings have been accepted,
 * each explicit factory-builder call replaces the whole interceptor configuration:
 * the last call wins, and passing {@code null} clears both global and scoped values.
 * These tests establish selection, identity, and creation counts without requiring CDI.
 * Container injection and destruction belong in the CDI integration tests.
 *
 * @author Steve Ebersole
 */
@JiraKey("HHH-12168")
class SessionFactoryInterceptorConfigurationTest {

	/// Different accepted value types exercise separate configuration-resolution paths.
	enum SettingForm {
		NONE, GLOBAL_INSTANCE, GLOBAL_CLASS, GLOBAL_CLASS_NAME, SCOPED_CLASS, SCOPED_CLASS_NAME, SCOPED_SUPPLIER;

		boolean isGlobal() {
			return this == GLOBAL_INSTANCE || this == GLOBAL_CLASS || this == GLOBAL_CLASS_NAME;
		}
	}

	/// DEFAULT leaves settings untouched. Both CLEAR actions explicitly remove all
	/// interceptor configuration; they are not requests to fall back to settings.
	/// The factory builder's historical `applyStatelessInterceptor` name means
	/// session-scoped creation for both stateful and stateless sessions.
	enum BuilderAction { DEFAULT, INSTANCE, SCOPED_CLASS, SCOPED_SUPPLIER, CLEAR_GLOBAL, CLEAR_SCOPED }

	static Stream<Arguments> configurationCells() {
		final List<Arguments> cells = new ArrayList<>();
		for ( var setting : SettingForm.values() ) {
			for ( var action : BuilderAction.values() ) {
				for ( var stateful : new boolean[] { true, false } ) {
					cells.add( Arguments.of( setting, action, stateful ) );
				}
			}
		}
		return cells.stream();
	}

	/// Crosses each settings value form with each factory-builder operation and
	/// both session kinds. Two sessions distinguish global reuse from scoped creation;
	/// distinct marker classes distinguish settings from builder replacements.
	@ParameterizedTest(name = "{0}, builder={1}, stateful={2}")
	@MethodSource("configurationCells")
	void settingFormsAndBuilderOverrides(SettingForm form, BuilderAction action, boolean stateful) {
		final var settingSupplierCalls = new AtomicInteger();
		final var builderSupplierCalls = new AtomicInteger();
		final var suppliedGlobal = new SettingsInterceptor();
		final var suppliedOverride = new BuilderInterceptor();
		final Map<String, Object> settings = switch ( form ) {
			case NONE -> Map.of();
			case GLOBAL_INSTANCE -> Map.of( INTERCEPTOR, suppliedGlobal );
			case GLOBAL_CLASS -> Map.of( INTERCEPTOR, SettingsInterceptor.class );
			case GLOBAL_CLASS_NAME -> Map.of( INTERCEPTOR, SettingsInterceptor.class.getName() );
			case SCOPED_CLASS -> Map.of( SESSION_SCOPED_INTERCEPTOR, SettingsInterceptor.class );
			case SCOPED_CLASS_NAME -> Map.of( SESSION_SCOPED_INTERCEPTOR, SettingsInterceptor.class.getName() );
			case SCOPED_SUPPLIER -> Map.of( SESSION_SCOPED_INTERCEPTOR, (Supplier<Interceptor>) () -> {
				settingSupplierCalls.incrementAndGet();
				return new SettingsInterceptor();
			} );
		};
		// Exclude the instances explicitly constructed by this test from Hibernate's counts.
		final int settingsCreationsBeforeBootstrap = SettingsInterceptor.creations;
		final int builderCreationsBeforeBootstrap = BuilderInterceptor.creations;
		withSessionFactory( settings, builder -> {
			switch ( action ) {
				case DEFAULT -> {}
				case INSTANCE -> builder.applyInterceptor( suppliedOverride );
				case CLEAR_GLOBAL -> builder.applyInterceptor( null );
				case CLEAR_SCOPED -> builder.applyStatelessInterceptor( (Supplier<Interceptor>) null );
				case SCOPED_CLASS -> builder.applyStatelessInterceptor( BuilderInterceptor.class );
				case SCOPED_SUPPLIER -> builder.applyStatelessInterceptor( (Supplier<Interceptor>) () -> {
					builderSupplierCalls.incrementAndGet();
					return new BuilderInterceptor();
				} );
			}
		}, factory -> {
			if ( !form.isGlobal() ) {
				assertThat( SettingsInterceptor.creations ).isEqualTo( settingsCreationsBeforeBootstrap );
			}
			assertThat( BuilderInterceptor.creations ).isEqualTo( builderCreationsBeforeBootstrap );
			// No scoped acquisition is permitted while building the factory.
			assertThat( settingSupplierCalls.get() ).isZero();
			assertThat( builderSupplierCalls.get() ).isZero();
			// A global class setting may already have been instantiated before the builder
			// overrides it. Require no further creation from that discarded configuration.
			final int settingsCreations = SettingsInterceptor.creations;
			final int builderCreations = BuilderInterceptor.creations;
			final boolean useGlobal = form.isGlobal() && action == BuilderAction.DEFAULT;
			final boolean useScopedSetting = !form.isGlobal() && form != SettingForm.NONE && action == BuilderAction.DEFAULT;
			final boolean useScopedBuilder = action == BuilderAction.SCOPED_CLASS || action == BuilderAction.SCOPED_SUPPLIER;
			try ( var first = openSession( factory, stateful ); var second = openSession( factory, stateful ) ) {
				if ( action == BuilderAction.INSTANCE ) {
					assertThat( first.getInterceptor() ).isSameAs( suppliedOverride );
					assertThat( second.getInterceptor() ).isSameAs( suppliedOverride );
				}
				else if ( useGlobal ) {
					assertThat( first.getInterceptor() ).isExactlyInstanceOf( SettingsInterceptor.class );
					assertThat( second.getInterceptor() ).isSameAs( first.getInterceptor() );
					if ( form == SettingForm.GLOBAL_INSTANCE ) {
						assertThat( first.getInterceptor() ).isSameAs( suppliedGlobal );
					}
				}
				else if ( useScopedSetting || useScopedBuilder ) {
					final var expectedClass = useScopedSetting ? SettingsInterceptor.class : BuilderInterceptor.class;
					assertThat( first.getInterceptor() ).isExactlyInstanceOf( expectedClass );
					assertThat( second.getInterceptor() ).isExactlyInstanceOf( expectedClass ).isNotSameAs( first.getInterceptor() );
				}
				else {
					assertThat( first.getInterceptor() ).isSameAs( EmptyInterceptor.INSTANCE );
					assertThat( second.getInterceptor() ).isSameAs( EmptyInterceptor.INSTANCE );
				}
			}
			if ( useGlobal && form != SettingForm.GLOBAL_INSTANCE ) {
				// Global resolution may be eager or deferred, but must produce only one instance.
				assertThat( SettingsInterceptor.creations - settingsCreationsBeforeBootstrap ).isEqualTo( 1 );
			}
			else {
				assertThat( SettingsInterceptor.creations - settingsCreations ).isEqualTo( useScopedSetting ? 2 : 0 );
			}
			assertThat( BuilderInterceptor.creations - builderCreations ).isEqualTo( useScopedBuilder ? 2 : 0 );
			assertThat( settingSupplierCalls.get() ).isEqualTo( useScopedSetting && form == SettingForm.SCOPED_SUPPLIER ? 2 : 0 );
			assertThat( builderSupplierCalls.get() ).isEqualTo( useScopedBuilder && action == BuilderAction.SCOPED_SUPPLIER ? 2 : 0 );
		} );
	}

	static Stream<Arguments> builderCallSequences() {
		return Stream.of( BuilderAction.INSTANCE, BuilderAction.SCOPED_CLASS, BuilderAction.SCOPED_SUPPLIER )
				.flatMap( first -> Stream.of( BuilderAction.values() )
						.filter( last -> last != BuilderAction.DEFAULT )
						.flatMap( last -> Stream.of( true, false ).map( stateful -> Arguments.of( first, last, stateful ) ) ) );
	}

	/// Exercises replacement within a scope, replacement across scopes, and clearing
	/// after each configuration form. An overwritten scoped creator must never run.
	@ParameterizedTest(name = "{0} then {1}, stateful={2}")
	@MethodSource("builderCallSequences")
	void lastBuilderCallReplacesEarlierConfiguration(BuilderAction first, BuilderAction last, boolean stateful) {
		final var original = new SettingsInterceptor();
		final var replacement = new BuilderInterceptor();
		final var originalSupplierCalls = new AtomicInteger();
		final var replacementSupplierCalls = new AtomicInteger();
		final int originalCreations = SettingsInterceptor.creations;
		final int replacementCreations = BuilderInterceptor.creations;
		withSessionFactory( Map.of(), builder -> {
			switch ( first ) {
				case INSTANCE -> builder.applyInterceptor( original );
				case SCOPED_CLASS -> builder.applyStatelessInterceptor( SettingsInterceptor.class );
				case SCOPED_SUPPLIER -> builder.applyStatelessInterceptor( (Supplier<Interceptor>) () -> {
					originalSupplierCalls.incrementAndGet();
					return new SettingsInterceptor();
				} );
				default -> throw new IllegalArgumentException( first.name() );
			}
			switch ( last ) {
				case INSTANCE -> builder.applyInterceptor( replacement );
				case SCOPED_CLASS -> builder.applyStatelessInterceptor( BuilderInterceptor.class );
				case SCOPED_SUPPLIER -> builder.applyStatelessInterceptor( (Supplier<Interceptor>) () -> {
					replacementSupplierCalls.incrementAndGet();
					return new BuilderInterceptor();
				} );
				case CLEAR_GLOBAL -> builder.applyInterceptor( null );
				case CLEAR_SCOPED -> builder.applyStatelessInterceptor( (Supplier<Interceptor>) null );
				default -> throw new IllegalArgumentException( last.name() );
			}
		}, factory -> {
			final boolean scoped = last == BuilderAction.SCOPED_CLASS || last == BuilderAction.SCOPED_SUPPLIER;
			try ( var session1 = openSession( factory, stateful ); var session2 = openSession( factory, stateful ) ) {
				if ( last == BuilderAction.INSTANCE ) {
					assertThat( session1.getInterceptor() ).isSameAs( replacement );
					assertThat( session2.getInterceptor() ).isSameAs( replacement );
				}
				else if ( scoped ) {
					assertThat( session1.getInterceptor() ).isExactlyInstanceOf( BuilderInterceptor.class );
					assertThat( session2.getInterceptor() ).isExactlyInstanceOf( BuilderInterceptor.class )
							.isNotSameAs( session1.getInterceptor() );
				}
				else {
					assertThat( session1.getInterceptor() ).isSameAs( EmptyInterceptor.INSTANCE );
					assertThat( session2.getInterceptor() ).isSameAs( EmptyInterceptor.INSTANCE );
				}
			}
			assertThat( originalSupplierCalls.get() ).isZero();
			assertThat( SettingsInterceptor.creations ).isEqualTo( originalCreations );
			assertThat( BuilderInterceptor.creations - replacementCreations ).isEqualTo( scoped ? 2 : 0 );
			assertThat( replacementSupplierCalls.get() ).isEqualTo( last == BuilderAction.SCOPED_SUPPLIER ? 2 : 0 );
		} );
	}

	static Stream<Arguments> conflictingSettings() {
		return Stream.of( SettingForm.GLOBAL_INSTANCE, SettingForm.GLOBAL_CLASS, SettingForm.GLOBAL_CLASS_NAME )
				.flatMap( global -> Stream.of( SettingForm.SCOPED_CLASS, SettingForm.SCOPED_CLASS_NAME, SettingForm.SCOPED_SUPPLIER )
						.map( scoped -> Arguments.of( global, scoped ) ) );
	}

	/// Conflicting settings are rejected regardless of their value types, before
	/// a SessionFactoryBuilder is available to make an explicit replacement.
	@ParameterizedTest(name = "{0} conflicts with {1}")
	@MethodSource("conflictingSettings")
	void conflictingReferenceFormsAreRejected(SettingForm global, SettingForm scoped) {
		final Object globalReference = switch ( global ) {
			case GLOBAL_INSTANCE -> new MarkerInterceptor();
			case GLOBAL_CLASS -> MarkerInterceptor.class;
			case GLOBAL_CLASS_NAME -> MarkerInterceptor.class.getName();
			default -> throw new IllegalArgumentException( global.name() );
		};
		final Object scopedReference = switch ( scoped ) {
			case SCOPED_CLASS -> MarkerInterceptor.class;
			case SCOPED_CLASS_NAME -> MarkerInterceptor.class.getName();
			case SCOPED_SUPPLIER -> (Supplier<Interceptor>) MarkerInterceptor::new;
			default -> throw new IllegalArgumentException( scoped.name() );
		};
		assertThrows( ConflictingInterceptorSettingsException.class, () -> withSessionFactory(
				Map.of( INTERCEPTOR, globalReference, SESSION_SCOPED_INTERCEPTOR, scopedReference ), factory -> {} ) );
	}

	private static SharedSessionContractImplementor openSession(SessionFactoryImplementor factory, boolean stateful) {
		return stateful ? factory.openSession() : (SharedSessionContractImplementor) factory.openStatelessSession();
	}

	public static class SettingsInterceptor implements Interceptor {
		static int creations;

		public SettingsInterceptor() {
			creations++;
		}
	}

	public static class BuilderInterceptor implements Interceptor {
		static int creations;

		public BuilderInterceptor() {
			creations++;
		}
	}

	// ---------------------------------------------------------------
	// [Settings] alone
	// ---------------------------------------------------------------

	@Test
	void neitherSettingConfigured_DefaultsToEmptyInterceptor() {
		withSessionFactory( Map.of(), sessionFactory -> {
			try ( var session = openSession( sessionFactory ) ) {
				assertThat( session.getInterceptor() ).isSameAs( EmptyInterceptor.INSTANCE );
			}
		} );
	}

	@Test
	void interceptorSettingAlone_IsASingletonSharedByAllSessions() {
		withSessionFactory( Map.of( INTERCEPTOR, MarkerInterceptor.class ), sessionFactory -> {
			try ( var session1 = openSession( sessionFactory );
					var session2 = openSession( sessionFactory ) ) {
				assertThat( session1.getInterceptor() ).isInstanceOf( MarkerInterceptor.class );
				assertThat( session1.getInterceptor() ).isSameAs( session2.getInterceptor() );
			}
		} );
	}

	@Test
	void sessionScopedInterceptorSettingAlone_CreatesAFreshInstancePerSession() {
		withSessionFactory( Map.of( SESSION_SCOPED_INTERCEPTOR, StatefulInterceptor.class ), sessionFactory -> {
			try ( var session1 = openSession( sessionFactory );
					var session2 = openSession( sessionFactory ) ) {
				assertThat( session1.getInterceptor() ).isInstanceOf( StatefulInterceptor.class );
				assertThat( session2.getInterceptor() ).isInstanceOf( StatefulInterceptor.class );
				assertThat( session1.getInterceptor() ).isNotSameAs( session2.getInterceptor() );
			}
		} );
	}

	@Test
	void bothSettingsConfigured_ThrowsConflictingInterceptorSettingsException() {
		assertThrows(
				ConflictingInterceptorSettingsException.class,
				() -> withSessionFactory(
						Map.of( INTERCEPTOR, MarkerInterceptor.class, SESSION_SCOPED_INTERCEPTOR, StatefulInterceptor.class ),
						sessionFactory -> {}
				)
		);
	}

	// ---------------------------------------------------------------
	// [SessionFactoryBuilder] applyInterceptor(Interceptor)
	// ---------------------------------------------------------------

	@Test
	void applyInterceptor_WithNoSetting_IsTheOnlyActiveInterceptor() {
		final var marker = new MarkerInterceptor();
		withSessionFactory( Map.of(), b -> b.applyInterceptor( marker ), sessionFactory -> {
			try ( var session = openSession( sessionFactory ) ) {
				assertThat( session.getInterceptor() ).isSameAs( marker );
			}
		} );
	}

	@Test
	void applyInterceptor_OverridesInterceptorSetting() {
		final var marker = new MarkerInterceptor();
		withSessionFactory(
				Map.of( INTERCEPTOR, MarkerInterceptor.class ),
				b -> b.applyInterceptor( marker ),
				sessionFactory -> {
					try ( var session = openSession( sessionFactory ) ) {
						assertThat( session.getInterceptor() ).isSameAs( marker );
					}
				}
		);
	}

	@Test
	void applyInterceptor_OverridesSessionScopedInterceptorSetting() {
		final var marker = new MarkerInterceptor();
		withSessionFactory(
				Map.of( SESSION_SCOPED_INTERCEPTOR, StatefulInterceptor.class ),
				b -> b.applyInterceptor( marker ),
				sessionFactory -> {
					try ( var session1 = openSession( sessionFactory );
							var session2 = openSession( sessionFactory ) ) {
						assertThat( session1.getInterceptor() ).isSameAs( marker );
						assertThat( session2.getInterceptor() ).isSameAs( marker );
					}
				}
		);
	}

	// ---------------------------------------------------------------
	// [SessionFactoryBuilder] applyStatelessInterceptor(Class)
	// ---------------------------------------------------------------

	@Test
	void applyStatelessInterceptorClass_WithNoSetting_CreatesAFreshInstancePerSession() {
		withSessionFactory(
				Map.of(),
				b -> b.applyStatelessInterceptor( StatefulInterceptor.class ),
				sessionFactory -> {
					try ( var session1 = openSession( sessionFactory );
							var session2 = openSession( sessionFactory ) ) {
						assertThat( session1.getInterceptor() ).isInstanceOf( StatefulInterceptor.class );
						assertThat( session2.getInterceptor() ).isInstanceOf( StatefulInterceptor.class );
						assertThat( session1.getInterceptor() ).isNotSameAs( session2.getInterceptor() );
					}
				}
		);
	}

	@Test
	void applyStatelessInterceptorClass_OverridesInterceptorSetting() {
		withSessionFactory(
				Map.of( INTERCEPTOR, MarkerInterceptor.class ),
				b -> b.applyStatelessInterceptor( StatefulInterceptor.class ),
				sessionFactory -> {
					try ( var session = openSession( sessionFactory ) ) {
						assertThat( session.getInterceptor() ).isInstanceOf( StatefulInterceptor.class );
					}
				}
		);
	}

	@Test
	void applyStatelessInterceptorClass_OverridesSessionScopedInterceptorSetting() {
		withSessionFactory(
				Map.of( SESSION_SCOPED_INTERCEPTOR, StatefulInterceptor.class ),
				b -> b.applyStatelessInterceptor( MarkerInterceptor.class ),
				sessionFactory -> {
					try ( var session = openSession( sessionFactory ) ) {
						assertThat( session.getInterceptor() ).isInstanceOf( MarkerInterceptor.class );
					}
				}
		);
	}

	// ---------------------------------------------------------------
	// [SessionFactoryBuilder] applyStatelessInterceptor(Supplier)
	// ---------------------------------------------------------------

	@Test
	void applyStatelessInterceptorSupplier_WithNoSetting_CreatesAFreshInstancePerSession() {
		withSessionFactory(
				Map.of(),
				b -> b.applyStatelessInterceptor( (Supplier<Interceptor>) MarkerInterceptor::new ),
				sessionFactory -> {
					try ( var session1 = openSession( sessionFactory );
							var session2 = openSession( sessionFactory ) ) {
						assertThat( session1.getInterceptor() ).isInstanceOf( MarkerInterceptor.class );
						assertThat( session2.getInterceptor() ).isInstanceOf( MarkerInterceptor.class );
						assertThat( session1.getInterceptor() ).isNotSameAs( session2.getInterceptor() );
					}
				}
		);
	}

	@Test
	void applyStatelessInterceptorSupplier_OverridesInterceptorSetting() {
		withSessionFactory(
				Map.of( INTERCEPTOR, MarkerInterceptor.class ),
				b -> b.applyStatelessInterceptor( (Supplier<Interceptor>) StatefulInterceptor::new ),
				sessionFactory -> {
					try ( var session = openSession( sessionFactory ) ) {
						assertThat( session.getInterceptor() ).isInstanceOf( StatefulInterceptor.class );
					}
				}
		);
	}

	@Test
	void applyStatelessInterceptorSupplier_OverridesSessionScopedInterceptorSetting() {
		withSessionFactory(
				Map.of( SESSION_SCOPED_INTERCEPTOR, StatefulInterceptor.class ),
				b -> b.applyStatelessInterceptor( (Supplier<Interceptor>) MarkerInterceptor::new ),
				sessionFactory -> {
					try ( var session = openSession( sessionFactory ) ) {
						assertThat( session.getInterceptor() ).isInstanceOf( MarkerInterceptor.class );
					}
				}
		);
	}

	// ---------------------------------------------------------------
	// Audit findings (not in Steve's original ask, found while reading the source)
	// ---------------------------------------------------------------

	@Test
	void settingsScopedSupplierUnderGlobalInterceptorSetting_DoesNotBehaveAsASupplier() {
		// SessionEventSettings#INTERCEPTOR is resolved via StrategySelector, which only understands
		// an Interceptor instance, a Class, or a String name - not a bare Supplier<Interceptor>.
		assertThrows(
				StrategySelectionException.class,
				() -> withSessionFactory(
						Map.of( INTERCEPTOR, (Supplier<Interceptor>) MarkerInterceptor::new ),
						sessionFactory -> {}
				)
		);
	}

	@Test
	void settingsScopedSupplierUnderSessionScopedInterceptorSetting_BehavesAsASupplier() {
		// Unlike INTERCEPTOR above, SESSION_SCOPED_INTERCEPTOR explicitly special-cases a Supplier value.
		withSessionFactory(
				Map.of( SESSION_SCOPED_INTERCEPTOR, (Supplier<Interceptor>) MarkerInterceptor::new ),
				sessionFactory -> {
					try ( var session1 = openSession( sessionFactory );
							var session2 = openSession( sessionFactory ) ) {
						assertThat( session1.getInterceptor() ).isInstanceOf( MarkerInterceptor.class );
						assertThat( session2.getInterceptor() ).isInstanceOf( MarkerInterceptor.class );
						assertThat( session1.getInterceptor() ).isNotSameAs( session2.getInterceptor() );
					}
				}
		);
	}

	@Test
	void configurationSetInterceptorToEmptyInstance_DoesNotSuppressSettingsBasedInterceptor() {
		// Configuration#interceptor defaults to EmptyInterceptor.INSTANCE, and Configuration's
		// buildSessionFactory() only forwards it to the builder when it's != EmptyInterceptor.INSTANCE.
		// So explicitly resetting to EmptyInterceptor.INSTANCE is silently indistinguishable from
		// "never configured" - it does NOT suppress a settings-based interceptor.
		final var configuration = new Configuration();
		configuration.setInterceptor( EmptyInterceptor.INSTANCE );
		configuration.addResource( "org/hibernate/orm/test/interceptor/User.hbm.xml" );

		// Note: the INTERCEPTOR setting must live on the registry, not Configuration#setProperty -
		// Configuration#buildSessionFactory(ServiceRegistry) never merges Configuration's own
		// properties map into an externally-supplied registry; only the no-arg overload does.
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySettings( Environment.getProperties() )
				.applySetting( INTERCEPTOR, MarkerInterceptor.class )
				.build();
		try ( var sessionFactory = (SessionFactoryImplementor) configuration.buildSessionFactory( ssr ) ) {
			try ( var session = openSession( sessionFactory ) ) {
				assertThat( session.getInterceptor() ).isInstanceOf( MarkerInterceptor.class );
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	// ---------------------------------------------------------------
	// Test infrastructure
	// ---------------------------------------------------------------

	private static SessionImplementor openSession(SessionFactoryImplementor sessionFactory) {
		return (SessionImplementor) sessionFactory.openSession();
	}

	private void withSessionFactory(Map<String, Object> settings, Consumer<SessionFactoryImplementor> work) {
		withSessionFactory( settings, b -> {}, work );
	}

	private void withSessionFactory(
			Map<String, Object> settings,
			Consumer<SessionFactoryBuilder> builderCustomizer,
			Consumer<SessionFactoryImplementor> work) {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySettings( Environment.getProperties() )
				.applySettings( settings )
				.build();
		try {
			final MetadataSources metadataSources = new MetadataSources( ssr )
					.addResource( "org/hibernate/orm/test/interceptor/User.hbm.xml" );
			final Metadata metadata = metadataSources.buildMetadata();
			final SessionFactoryBuilder sfBuilder = metadata.getSessionFactoryBuilder();
			builderCustomizer.accept( sfBuilder );
			try ( var sessionFactory = (SessionFactoryImplementor) sfBuilder.build() ) {
				work.accept( sessionFactory );
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	public static class MarkerInterceptor implements Interceptor {
	}
}
