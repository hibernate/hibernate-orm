package org.hibernate.orm.test.interceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.hibernate.Interceptor;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.creation.CommonBuilder;
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
 * Baseline (pre-CDI) {@code [CommonBuilder]} cross-check every
 * {@link CommonBuilder} interceptor action, crossed against every SessionFactory-level
 * interceptor end-state, for both stateful and stateless sessions opened directly from
 * the {@link org.hibernate.SessionFactory}.
 * <p>
 * Factory configuration is established separately by
 * {@link SessionFactoryInterceptorConfigurationTest}. Here, an explicit session
 * interceptor replaces the factory default, {@code noInterceptor()} suppresses
 * both scopes, and {@code noSessionInterceptorCreation()} suppresses only scoped
 * creation. An explicit {@code null} is equivalent to {@code noInterceptor()}.
 *
 * @author Steve Ebersole
 */
@JiraKey("HHH-12168")
class CommonBuilderInterceptorBaselineTest {

	/// Class-based and supplier-based scoped factories must obey the same rules,
	/// even when their acquisition paths differ.
	enum EndState {
		NONE, GLOBAL, SCOPED_CLASS, SCOPED_SUPPLIER;

		boolean isScoped() {
			return this == SCOPED_CLASS || this == SCOPED_SUPPLIER;
		}
	}

	enum Action { DEFAULT, EXPLICIT_INSTANCE, EXPLICIT_NULL, NO_INTERCEPTOR, NO_SESSION_INTERCEPTOR_CREATION }

	private static int scopedSupplierCalls;

	private static StandardServiceRegistry noneRegistry;
	private static StandardServiceRegistry globalRegistry;
	private static StandardServiceRegistry scopedClassRegistry;
	private static StandardServiceRegistry scopedSupplierRegistry;
	private static SessionFactoryImplementor noneFactory;
	private static SessionFactoryImplementor globalFactory;
	private static SessionFactoryImplementor scopedClassFactory;
	private static SessionFactoryImplementor scopedSupplierFactory;

	@BeforeAll
	static void buildSessionFactories() {
		noneRegistry = buildRegistry( Map.of() );
		noneFactory = buildSessionFactory( noneRegistry );

		globalRegistry = buildRegistry( Map.of( INTERCEPTOR, MarkerInterceptor.class ) );
		globalFactory = buildSessionFactory( globalRegistry );

		scopedClassRegistry = buildRegistry( Map.of( SESSION_SCOPED_INTERCEPTOR, CountingInterceptor.class ) );
		scopedClassFactory = buildSessionFactory( scopedClassRegistry );

		scopedSupplierRegistry = buildRegistry( Map.of( SESSION_SCOPED_INTERCEPTOR, (Supplier<Interceptor>) () -> {
			scopedSupplierCalls++;
			return new CountingInterceptor();
		} ) );
		scopedSupplierFactory = buildSessionFactory( scopedSupplierRegistry );
	}

	@AfterAll
	static void tearDown() {
		noneFactory.close();
		StandardServiceRegistryBuilder.destroy( noneRegistry );
		globalFactory.close();
		StandardServiceRegistryBuilder.destroy( globalRegistry );
		scopedClassFactory.close();
		StandardServiceRegistryBuilder.destroy( scopedClassRegistry );
		scopedSupplierFactory.close();
		StandardServiceRegistryBuilder.destroy( scopedSupplierRegistry );
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
			case SCOPED_CLASS -> scopedClassFactory;
			case SCOPED_SUPPLIER -> scopedSupplierFactory;
		};
	}

	static Stream<Arguments> matrixCells() {
		final List<Arguments> cells = new ArrayList<>();
		for ( var state : EndState.values() ) {
			for ( var action : Action.values() ) {
				for ( var stateful : new boolean[] { true, false } ) {
					cells.add( Arguments.of( state, action, stateful ) );
				}
			}
		}
		return cells.stream();
	}

	@ParameterizedTest(name = "[{index}] {0} x {1} x stateful={2}")
	@MethodSource("matrixCells")
	void crossProduct(EndState state, Action action, boolean stateful) {
		final var sessionFactory = factoryFor( state );
		final var explicitInstance = new MarkerInterceptor();
		final CommonBuilder builder = stateful ? sessionFactory.withOptions() : sessionFactory.withStatelessOptions();

		final int callsBefore = scopedSupplierCalls;
		final int creationsBefore = CountingInterceptor.creations;
		try ( var session = openWithAction( builder, action, explicitInstance ) ) {
			final var resolved = session.getInterceptor();
			switch ( action ) {
				case DEFAULT -> assertDefaultResolution( state, sessionFactory, resolved );
				case EXPLICIT_INSTANCE -> assertThat( resolved ).isSameAs( explicitInstance );
				case EXPLICIT_NULL, NO_INTERCEPTOR -> assertThat( resolved ).isSameAs( EmptyInterceptor.INSTANCE );
				case NO_SESSION_INTERCEPTOR_CREATION -> assertNoSessionInterceptorCreationResolution( state, sessionFactory, resolved );
			}
		}
		// Checking the returned instance alone would miss a scoped interceptor that
		// was unnecessarily created and discarded when overridden or suppressed.
		final int expectedCreations = state.isScoped() && action == Action.DEFAULT ? 1 : 0;
		assertThat( scopedSupplierCalls - callsBefore ).isEqualTo( state == EndState.SCOPED_SUPPLIER ? expectedCreations : 0 );
		assertThat( CountingInterceptor.creations - creationsBefore ).isEqualTo( expectedCreations );
	}

	private void assertDefaultResolution(EndState state, SessionFactoryImplementor sessionFactory, Interceptor resolved) {
		switch ( state ) {
			case NONE -> assertThat( resolved ).isSameAs( EmptyInterceptor.INSTANCE );
			case GLOBAL -> assertThat( resolved ).isSameAs( sessionFactory.getInterceptorStrategy().getFactoryInterceptor() );
			case SCOPED_CLASS, SCOPED_SUPPLIER -> assertThat( resolved ).isInstanceOf( CountingInterceptor.class );
		}
	}

	private void assertNoSessionInterceptorCreationResolution(
			EndState state, SessionFactoryImplementor sessionFactory, Interceptor resolved) {
		switch ( state ) {
			// Suppressing session-scoped creation leaves the global interceptor active.
			case NONE -> assertThat( resolved ).isSameAs( EmptyInterceptor.INSTANCE );
			case GLOBAL -> assertThat( resolved ).isSameAs( sessionFactory.getInterceptorStrategy().getFactoryInterceptor() );
			case SCOPED_CLASS, SCOPED_SUPPLIER -> assertThat( resolved ).isSameAs( EmptyInterceptor.INSTANCE );
		}
	}

	private static SharedSessionContractImplementor openWithAction(
			CommonBuilder builder, Action action, Interceptor explicitInstance) {
		switch ( action ) {
			case DEFAULT -> {}
			case EXPLICIT_INSTANCE -> builder.interceptor( explicitInstance );
			case EXPLICIT_NULL -> builder.interceptor( null );
			case NO_INTERCEPTOR -> builder.noInterceptor();
			case NO_SESSION_INTERCEPTOR_CREATION -> builder.noSessionInterceptorCreation();
		}
		return (SharedSessionContractImplementor) builder.open();
	}

	// ---------------------------------------------------------------
	// Order-independence: applying a non-default action on one builder must not leak
	// into, or be affected by, a sibling builder opened (before or after it) from the
	// same SessionFactory. Uses the NONE-state factory so each sibling's own expected
	// result is as simple as possible to state independently of the other.
	// ---------------------------------------------------------------

	@Test
	void explicitInstanceDoesNotLeakAcrossSiblings_Stateful() {
		assertSiblingIndependence( true, Action.EXPLICIT_INSTANCE );
	}

	@Test
	void explicitInstanceDoesNotLeakAcrossSiblings_Stateless() {
		assertSiblingIndependence( false, Action.EXPLICIT_INSTANCE );
	}

	@Test
	void explicitNullDoesNotLeakAcrossSiblings_Stateful() {
		assertSiblingIndependence( true, Action.EXPLICIT_NULL );
	}

	@Test
	void explicitNullDoesNotLeakAcrossSiblings_Stateless() {
		assertSiblingIndependence( false, Action.EXPLICIT_NULL );
	}

	@Test
	void noInterceptorDoesNotLeakAcrossSiblings_Stateful() {
		assertSiblingIndependence( true, Action.NO_INTERCEPTOR );
	}

	@Test
	void noInterceptorDoesNotLeakAcrossSiblings_Stateless() {
		assertSiblingIndependence( false, Action.NO_INTERCEPTOR );
	}

	@Test
	void noSessionInterceptorCreationDoesNotLeakAcrossSiblings_Stateful() {
		assertSiblingIndependence( true, Action.NO_SESSION_INTERCEPTOR_CREATION );
	}

	@Test
	void noSessionInterceptorCreationDoesNotLeakAcrossSiblings_Stateless() {
		assertSiblingIndependence( false, Action.NO_SESSION_INTERCEPTOR_CREATION );
	}

	private void assertSiblingIndependence(boolean stateful, Action action) {
		final var explicitInstance = new MarkerInterceptor();

		// default opened first, then the custom action
		try ( var defaultSession = openWithAction( newBuilder( stateful ), Action.DEFAULT, explicitInstance );
				var customSession = openWithAction( newBuilder( stateful ), action, explicitInstance ) ) {
			assertThat( defaultSession.getInterceptor() ).isSameAs( EmptyInterceptor.INSTANCE );
			assertCustomActionResolution( action, explicitInstance, customSession.getInterceptor() );
		}

		// custom action opened first, then default
		try ( var customSession = openWithAction( newBuilder( stateful ), action, explicitInstance );
				var defaultSession = openWithAction( newBuilder( stateful ), Action.DEFAULT, explicitInstance ) ) {
			assertCustomActionResolution( action, explicitInstance, customSession.getInterceptor() );
			assertThat( defaultSession.getInterceptor() ).isSameAs( EmptyInterceptor.INSTANCE );
		}
	}

	private CommonBuilder newBuilder(boolean stateful) {
		return stateful ? noneFactory.withOptions() : noneFactory.withStatelessOptions();
	}

	private void assertCustomActionResolution(Action action, Interceptor explicitInstance, Interceptor resolved) {
		switch ( action ) {
			case EXPLICIT_INSTANCE -> assertThat( resolved ).isSameAs( explicitInstance );
			case EXPLICIT_NULL, NO_INTERCEPTOR, NO_SESSION_INTERCEPTOR_CREATION ->
					assertThat( resolved ).isSameAs( EmptyInterceptor.INSTANCE );
			default -> throw new IllegalStateException( "Unexpected action: " + action );
		}
	}

	public static class MarkerInterceptor implements Interceptor {
	}

	public static class CountingInterceptor implements Interceptor {
		static int creations;

		public CountingInterceptor() {
			creations++;
		}
	}
}
