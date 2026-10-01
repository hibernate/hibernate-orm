/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interceptor;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
import org.hibernate.internal.EmptyInterceptor;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.SessionEventSettings.INTERCEPTOR;
import static org.hibernate.cfg.SessionEventSettings.SESSION_SCOPED_INTERCEPTOR;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Baseline (pre-CDI) cross-check of how {@code SessionEventSettings#INTERCEPTOR} /
 * {@code #SESSION_SCOPED_INTERCEPTOR} and the corresponding {@link SessionFactoryBuilder}
 * methods interact when resolving the {@link Interceptor} used by sessions opened
 * directly from the {@link org.hibernate.SessionFactory}.
 */
@JiraKey("HHH-12168")
class SessionFactoryInterceptorConfigurationTest {

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
	void applyStatelessInterceptorClass_DoesNotOverrideInterceptorSetting() {
		// Known baseline-vs-CDI divergence: a global (settings-based) interceptor always wins over
		// a session-scoped supplier, regardless of how the supplier was configured. The CDI branch's
		// InterceptorStrategy deliberately changes this.
		withSessionFactory(
				Map.of( INTERCEPTOR, MarkerInterceptor.class ),
				b -> b.applyStatelessInterceptor( StatefulInterceptor.class ),
				sessionFactory -> {
					try ( var session = openSession( sessionFactory ) ) {
						assertThat( session.getInterceptor() ).isInstanceOf( MarkerInterceptor.class );
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
	void applyStatelessInterceptorSupplier_DoesNotOverrideInterceptorSetting() {
		withSessionFactory(
				Map.of( INTERCEPTOR, MarkerInterceptor.class ),
				b -> b.applyStatelessInterceptor( (Supplier<Interceptor>) StatefulInterceptor::new ),
				sessionFactory -> {
					try ( var session = openSession( sessionFactory ) ) {
						assertThat( session.getInterceptor() ).isInstanceOf( MarkerInterceptor.class );
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
