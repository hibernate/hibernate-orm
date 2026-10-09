package org.hibernate.orm.test.interceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;

import org.hibernate.CacheMode;
import org.hibernate.Interceptor;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.EmptyInterceptor;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.SessionEventSettings.INTERCEPTOR;
import static org.hibernate.cfg.SessionEventSettings.SESSION_SCOPED_INTERCEPTOR;

/**
 * Baseline (pre-CDI) coverage of a genuinely separate interceptor entry point: an
 * {@link Interceptor} passed as a Jakarta Persistence 4
 * {@link EntityManager.CreationOption}/{@code EntityAgent.CreationOption} vararg to
 * {@link org.hibernate.SessionFactory#createEntityManager(EntityManager.CreationOption...)} /
 * {@code #createEntityAgent(EntityAgent.CreationOption...)}. This is auto-detected purely
 * by type (via {@code OptionsHelper#applyOption}) rather than through a dedicated builder
 * method, and routes through a completely different code path than
 * {@link CommonBuilderInterceptorBaselineTest} / {@link CommonSharedBuilderInterceptorBaselineTest}.
 */
@JiraKey("HHH-12168")
class CreationOptionInterceptorBaselineTest {

	enum EndState { NONE, GLOBAL, SCOPED }

	enum EntryPoint { ENTITY_MANAGER, ENTITY_AGENT }

	private static StandardServiceRegistry noneRegistry;
	private static StandardServiceRegistry globalRegistry;
	private static StandardServiceRegistry scopedRegistry;
	private static SessionFactoryImplementor noneFactory;
	private static SessionFactoryImplementor globalFactory;
	private static SessionFactoryImplementor scopedFactory;

	@BeforeAll
	static void buildSessionFactories() {
		noneRegistry = buildRegistry( Map.of() );
		noneFactory = buildSessionFactory( noneRegistry );

		globalRegistry = buildRegistry( Map.of( INTERCEPTOR, MarkerInterceptor.class ) );
		globalFactory = buildSessionFactory( globalRegistry );

		scopedRegistry = buildRegistry( Map.of( SESSION_SCOPED_INTERCEPTOR, StatefulInterceptor.class ) );
		scopedFactory = buildSessionFactory( scopedRegistry );
	}

	@AfterAll
	static void tearDown() {
		noneFactory.close();
		StandardServiceRegistryBuilder.destroy( noneRegistry );
		globalFactory.close();
		StandardServiceRegistryBuilder.destroy( globalRegistry );
		scopedFactory.close();
		StandardServiceRegistryBuilder.destroy( scopedRegistry );
	}

	private static StandardServiceRegistry buildRegistry(Map<String, Object> settings) {
		return ServiceRegistryUtil.serviceRegistryBuilder()
				.applySettings( Environment.getProperties() )
				.applySettings( settings )
				.build();
	}

	private static SessionFactoryImplementor buildSessionFactory(StandardServiceRegistry ssr) {
		final Metadata metadata = new MetadataSources( ssr )
				.addResource( "org/hibernate/orm/test/interceptor/User.hbm.xml" )
				.buildMetadata();
		return (SessionFactoryImplementor) metadata.buildSessionFactory();
	}

	private static SessionFactoryImplementor factoryFor(EndState state) {
		return switch ( state ) {
			case NONE -> noneFactory;
			case GLOBAL -> globalFactory;
			case SCOPED -> scopedFactory;
		};
	}

	private static SharedSessionContractImplementor openWithInterceptor(
			EntryPoint entryPoint, SessionFactoryImplementor sessionFactory, Interceptor interceptor) {
		return switch ( entryPoint ) {
			case ENTITY_MANAGER -> (SharedSessionContractImplementor) sessionFactory.createEntityManager( interceptor );
			case ENTITY_AGENT -> (SharedSessionContractImplementor) sessionFactory.createEntityAgent( interceptor );
		};
	}

	// A non-interceptor "filler" option forces createEntityAgent through its
	// options-based construction path rather than its zero-arg fast path (see
	// SessionFactoryImpl#createEntityAgent(EntityAgent.CreationOption...)), so this still
	// exercises the OptionsHelper-based default resolution, not the CommonBuilder one
	// already covered by CommonBuilderInterceptorBaselineTest.
	private static SharedSessionContractImplementor openWithFillerOnly(EntryPoint entryPoint, SessionFactoryImplementor sessionFactory) {
		return switch ( entryPoint ) {
			case ENTITY_MANAGER -> (SharedSessionContractImplementor) sessionFactory.createEntityManager( CacheMode.IGNORE );
			case ENTITY_AGENT -> (SharedSessionContractImplementor) sessionFactory.createEntityAgent( CacheMode.IGNORE );
		};
	}

	static Stream<Arguments> stateAndEntryPoint() {
		final List<Arguments> cells = new ArrayList<>();
		for ( var state : EndState.values() ) {
			for ( var entryPoint : EntryPoint.values() ) {
				cells.add( Arguments.of( state, entryPoint ) );
			}
		}
		return cells.stream();
	}

	// ---------------------------------------------------------------
	// 1. An interceptor passed as a CreationOption vararg is honored, and overrides the
	// SF-level state the same way an explicit CommonBuilder#interceptor(Interceptor) does.
	// ---------------------------------------------------------------

	@ParameterizedTest(name = "[{index}] {0} x {1}")
	@MethodSource("stateAndEntryPoint")
	void explicitInterceptorCreationOptionIsHonored(EndState state, EntryPoint entryPoint) {
		final var explicitInstance = new MarkerInterceptor();
		try ( var session = openWithInterceptor( entryPoint, factoryFor( state ), explicitInstance ) ) {
			assertThat( session.getInterceptor() ).isSameAs( explicitInstance );
		}
	}

	@ParameterizedTest(name = "[{index}] {0} x {1}")
	@MethodSource("stateAndEntryPoint")
	void noInterceptorCreationOptionFallsBackToSfState(EndState state, EntryPoint entryPoint) {
		final var sessionFactory = factoryFor( state );
		try ( var session = openWithFillerOnly( entryPoint, sessionFactory ) ) {
			final var resolved = session.getInterceptor();
			switch ( state ) {
				case NONE -> assertThat( resolved ).isSameAs( EmptyInterceptor.INSTANCE );
				case GLOBAL -> assertThat( resolved ).isSameAs( sessionFactory.getInterceptorStrategy().getFactoryInterceptor() );
				case SCOPED -> assertThat( resolved ).isInstanceOf( StatefulInterceptor.class );
			}
		}
	}

	@Test
	void interceptorMixedWithCacheModeIsHonored_CreateEntityManager() {
		final var explicitInstance = new MarkerInterceptor();
		try ( var session = (SharedSessionContractImplementor)
				noneFactory.createEntityManager( explicitInstance, CacheMode.IGNORE ) ) {
			assertThat( session.getInterceptor() ).isSameAs( explicitInstance );
			assertThat( session.getCacheMode() ).isEqualTo( CacheMode.IGNORE );
		}
	}

	@Test
	void interceptorMixedWithCacheModeIsHonored_CreateEntityAgent() {
		final var explicitInstance = new MarkerInterceptor();
		try ( var session = (SharedSessionContractImplementor)
				noneFactory.createEntityAgent( explicitInstance, CacheMode.IGNORE ) ) {
			assertThat( session.getInterceptor() ).isSameAs( explicitInstance );
			assertThat( session.getCacheMode() ).isEqualTo( CacheMode.IGNORE );
		}
	}

	// ---------------------------------------------------------------
	// 2. Negative: the Map-based overloads only ever special-case HINT_TENANT_ID - an
	// Interceptor stuffed into the map under any other key must be silently ignored.
	// ---------------------------------------------------------------

	@Test
	void mapBasedCreateEntityManagerDoesNotPickUpInterceptor() {
		final var smuggledInterceptor = new MarkerInterceptor();
		try ( var session = (SharedSessionContractImplementor)
				noneFactory.createEntityManager( Map.of( "someArbitraryKey", smuggledInterceptor ) ) ) {
			assertThat( session.getInterceptor() ).isSameAs( EmptyInterceptor.INSTANCE );
		}
	}

	@Test
	void mapBasedCreateEntityAgentDoesNotPickUpInterceptor() {
		final var smuggledInterceptor = new MarkerInterceptor();
		try ( var session = (SharedSessionContractImplementor)
				noneFactory.createEntityAgent( Map.of( "someArbitraryKey", smuggledInterceptor ) ) ) {
			assertThat( session.getInterceptor() ).isSameAs( EmptyInterceptor.INSTANCE );
		}
	}

	// ---------------------------------------------------------------
	// 3. Parity: createEntityAgent()'s zero-arg fast path and createEntityAgent(options...)'s
	// options-collector path must resolve the SF-level interceptor identically.
	// ---------------------------------------------------------------

	@Test
	void zeroArgAndOptionsArgCreateEntityAgent_ResolveSameGlobalInterceptor() {
		try ( var zeroArg = (SharedSessionContractImplementor) globalFactory.createEntityAgent();
				var withOptions = (SharedSessionContractImplementor) globalFactory.createEntityAgent( CacheMode.IGNORE ) ) {
			assertThat( zeroArg.getInterceptor() ).isSameAs( withOptions.getInterceptor() );
			assertThat( zeroArg.getInterceptor() ).isSameAs( globalFactory.getInterceptorStrategy().getFactoryInterceptor() );
		}
	}

	@Test
	void zeroArgAndOptionsArgCreateEntityAgent_BothCreateFreshScopedInterceptor() {
		try ( var zeroArg = (SharedSessionContractImplementor) scopedFactory.createEntityAgent();
				var withOptions = (SharedSessionContractImplementor) scopedFactory.createEntityAgent( CacheMode.IGNORE ) ) {
			assertThat( zeroArg.getInterceptor() ).isInstanceOf( StatefulInterceptor.class );
			assertThat( withOptions.getInterceptor() ).isInstanceOf( StatefulInterceptor.class );
			assertThat( zeroArg.getInterceptor() ).isNotSameAs( withOptions.getInterceptor() );
		}
	}

	public static class MarkerInterceptor implements Interceptor {
	}
}
