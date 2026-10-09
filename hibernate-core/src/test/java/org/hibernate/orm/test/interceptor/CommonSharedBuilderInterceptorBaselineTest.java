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
import org.hibernate.engine.creation.CommonSharedBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.EmptyInterceptor;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.SessionEventSettings.INTERCEPTOR;
import static org.hibernate.cfg.SessionEventSettings.SESSION_SCOPED_INTERCEPTOR;

/// Baselines factory configuration, parent interceptor state, and child builder actions
/// across all stateful/stateless parent and child combinations.
///
/// Factory state and parent state are independent axes: a parent may use a factory
/// default, replace it, or suppress it. A child uses factory defaults unless it
/// explicitly supplies or shares an interceptor. These tests establish identity
/// and acquisition behavior; CDI destruction will require separate lifecycle tests.
///
/// @author Steve Ebersole
@JiraKey("HHH-12168")
class CommonSharedBuilderInterceptorBaselineTest {

	/// Keep both scoped acquisition paths in the matrix, since sharing and suppression
	/// must avoid acquisition through either path.
	enum EndState {
		NONE, GLOBAL, SCOPED_CLASS, SCOPED_SUPPLIER;

		boolean isScoped() {
			return this == SCOPED_CLASS || this == SCOPED_SUPPLIER;
		}
	}

	enum ParentAction { DEFAULT, EXPLICIT_INSTANCE, EXPLICIT_NULL, NO_INTERCEPTOR, NO_SESSION_INTERCEPTOR_CREATION }

	enum ChildAction { DEFAULT, EXPLICIT_INSTANCE, EXPLICIT_NULL, NO_INTERCEPTOR, NO_SESSION_INTERCEPTOR_CREATION, SHARE_PARENT }

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
		return parentCells().flatMap( cell -> Stream.of( ChildAction.values() )
				.map( action -> Arguments.of( cell.get()[0], cell.get()[1], cell.get()[2], cell.get()[3], action ) ) );
	}

	static Stream<Arguments> parentCells() {
		final List<Arguments> cells = new ArrayList<>();
		for ( var state : EndState.values() ) {
			for ( var parentAction : ParentAction.values() ) {
				for ( var statefulParent : new boolean[] { true, false } ) {
					for ( var statefulChild : new boolean[] { true, false } ) {
						cells.add( Arguments.of( state, parentAction, statefulParent, statefulChild ) );
					}
				}
			}
		}
		return cells.stream();
	}

	/// A parent with an explicit interceptor is only one case. In particular, a parent
	/// with no active interceptor means sharing propagates that absence to the child,
	/// while a parent with a scoped interceptor lets us distinguish borrowing from
	/// acquiring another instance of the same class.
	@ParameterizedTest(name = "{0}, parent={1}, statefulParent={2}, statefulChild={3}, child={4}")
	@MethodSource("matrixCells")
	void crossProduct(EndState state, ParentAction parentAction, boolean statefulParent,
			boolean statefulChild, ChildAction action) {
		final var sessionFactory = factoryFor( state );
		final var childMarker = new MarkerInterceptor();
		try ( var parent = openParent( state, parentAction, statefulParent ) ) {
			final var parentInterceptor = parent.getInterceptor();
			final CommonSharedBuilder childBuilder =
					statefulChild ? parent.sessionWithOptions() : parent.statelessWithOptions();
			final int expectedCreations = state.isScoped() && action == ChildAction.DEFAULT ? 1 : 0;
			Interceptor previousChildInterceptor = null;
			// Reusing the builder must acquire a fresh scoped bean, but keep a shared instance.
			for ( int i = 0; i < 2; i++ ) {
				final int callsBefore = scopedSupplierCalls;
				final int creationsBefore = CountingInterceptor.creations;
				try ( var child = openChildWithAction( childBuilder, action, childMarker ) ) {
					final var resolved = child.getInterceptor();
					switch ( action ) {
						case DEFAULT -> assertDefaultResolution( state, sessionFactory, resolved );
						case EXPLICIT_INSTANCE -> assertThat( resolved ).isSameAs( childMarker );
						case EXPLICIT_NULL, NO_INTERCEPTOR -> assertThat( resolved ).isSameAs( EmptyInterceptor.INSTANCE );
						case NO_SESSION_INTERCEPTOR_CREATION -> assertNoSessionInterceptorCreationResolution( state, sessionFactory, resolved );
						case SHARE_PARENT ->
								// Sharing propagates the parent's resolved interceptor exactly,
								// including an explicit absence - it must never fall back to
								// factory defaults.
								assertThat( resolved ).isSameAs( parentInterceptor );
					}
					if ( expectedCreations == 1 ) {
						assertThat( resolved ).isNotSameAs( parentInterceptor ).isNotSameAs( previousChildInterceptor );
					}
					previousChildInterceptor = resolved;
				}
				// A borrowed interceptor must not be accompanied by an unused scoped allocation.
				assertThat( scopedSupplierCalls - callsBefore ).isEqualTo( state == EndState.SCOPED_SUPPLIER ? expectedCreations : 0 );
				assertThat( CountingInterceptor.creations - creationsBefore ).isEqualTo( expectedCreations );
				assertThat( parent.isOpen() ).isTrue();
				assertThat( parent.getInterceptor() ).isSameAs( parentInterceptor );
			}
		}
	}

	private SharedSessionContractImplementor openParent(EndState state, ParentAction action, boolean stateful) {
		final var factory = factoryFor( state );
		final CommonBuilder builder = stateful ? factory.withOptions() : factory.withStatelessOptions();
		final var explicit = new MarkerInterceptor();
		switch ( action ) {
			case DEFAULT -> {}
			case EXPLICIT_INSTANCE -> builder.interceptor( explicit );
			case EXPLICIT_NULL -> builder.interceptor( null );
			case NO_INTERCEPTOR -> builder.noInterceptor();
			case NO_SESSION_INTERCEPTOR_CREATION -> builder.noSessionInterceptorCreation();
		}
		final int callsBefore = scopedSupplierCalls;
		final int creationsBefore = CountingInterceptor.creations;
		final var parent = (SharedSessionContractImplementor) builder.open();
		try {
			switch ( action ) {
				case DEFAULT -> assertDefaultResolution( state, factory, parent.getInterceptor() );
				case EXPLICIT_INSTANCE -> assertThat( parent.getInterceptor() ).isSameAs( explicit );
				case EXPLICIT_NULL, NO_INTERCEPTOR -> assertThat( parent.getInterceptor() ).isSameAs( EmptyInterceptor.INSTANCE );
				case NO_SESSION_INTERCEPTOR_CREATION -> assertNoSessionInterceptorCreationResolution( state, factory, parent.getInterceptor() );
			}
			final int expectedCreations = state.isScoped() && action == ParentAction.DEFAULT ? 1 : 0;
			assertThat( scopedSupplierCalls - callsBefore ).isEqualTo( state == EndState.SCOPED_SUPPLIER ? expectedCreations : 0 );
			assertThat( CountingInterceptor.creations - creationsBefore ).isEqualTo( expectedCreations );
			return parent;
		}
		catch (RuntimeException | Error e) {
			parent.close();
			throw e;
		}
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
			case NONE -> assertThat( resolved ).isSameAs( EmptyInterceptor.INSTANCE );
			case GLOBAL -> assertThat( resolved ).isSameAs( sessionFactory.getInterceptorStrategy().getFactoryInterceptor() );
			case SCOPED_CLASS, SCOPED_SUPPLIER -> assertThat( resolved ).isSameAs( EmptyInterceptor.INSTANCE );
		}
	}

	private static SharedSessionContractImplementor openChildWithAction(
			CommonSharedBuilder builder, ChildAction action, Interceptor childMarker) {
		switch ( action ) {
			case DEFAULT -> {}
			case EXPLICIT_INSTANCE -> builder.interceptor( childMarker );
			case EXPLICIT_NULL -> builder.interceptor( null );
			case NO_INTERCEPTOR -> builder.noInterceptor();
			case NO_SESSION_INTERCEPTOR_CREATION -> builder.noSessionInterceptorCreation();
			case SHARE_PARENT -> builder.interceptor();
		}
		return (SharedSessionContractImplementor) builder.open();
	}

	/// Sharing an existing interceptor and disabling scoped creation are independent
	/// options, so their call order must not matter. Sharing always propagates the
	/// parent's resolved interceptor exactly - including an explicit absence - so
	/// `noSessionInterceptorCreation()` only matters when it is the sole option in play.
	@ParameterizedTest(name = "{0}, parent={1}, statefulParent={2}, statefulChild={3}")
	@MethodSource("parentCells")
	void shareParentAndNoSessionInterceptorCreation(
			EndState state, ParentAction parentAction, boolean statefulParent, boolean statefulChild) {
		try ( var parent = openParent( state, parentAction, statefulParent ) ) {
			final var expected = parent.getInterceptor();
			final int callsBefore = scopedSupplierCalls;
			final int creationsBefore = CountingInterceptor.creations;
			for ( var shareFirst : new boolean[] { true, false } ) {
				final CommonSharedBuilder builder =
						statefulChild ? parent.sessionWithOptions() : parent.statelessWithOptions();
				if ( shareFirst ) {
					builder.interceptor().noSessionInterceptorCreation();
				}
				else {
					builder.noSessionInterceptorCreation().interceptor();
				}
				try ( var child = (SharedSessionContractImplementor) builder.open() ) {
					assertThat( child.getInterceptor() ).isSameAs( expected );
				}
			}
			assertThat( scopedSupplierCalls ).isEqualTo( callsBefore );
			assertThat( CountingInterceptor.creations ).isEqualTo( creationsBefore );
		}
	}

	/// child calls nothing - resolves exactly as an independent session would,
	/// using the SessionFactory's default. The parent's empty Interceptor has
	/// no bearing, because the child never asked to share it.
	@ParameterizedTest(name = "{0}")
	@EnumSource(value = EndState.class, names = { "GLOBAL", "SCOPED_CLASS" })
	void parentHasNoInterceptor_childDefault_getsFactoryDefault(EndState state) {
		final var sessionFactory = factoryFor( state );
		try ( var parent = openParent( state, ParentAction.NO_INTERCEPTOR, true ) ) {
			assertThat( parent.getInterceptor() ).isSameAs( EmptyInterceptor.INSTANCE );
			try ( var child = (SharedSessionContractImplementor) parent.sessionWithOptions().open() ) {
				assertDefaultResolution( state, sessionFactory, child.getInterceptor() );
			}
		}
	}

	/// child calls noInterceptor() - never gets an Interceptor, regardless of
	/// what the SessionFactory has configured.
	@ParameterizedTest(name = "{0}")
	@EnumSource(value = EndState.class, names = { "GLOBAL", "SCOPED_CLASS" })
	void parentHasNoInterceptor_childNoInterceptor_neverGetsOne(EndState state) {
		try ( var parent = openParent( state, ParentAction.NO_INTERCEPTOR, true ) ) {
			try ( var child = (SharedSessionContractImplementor) parent.sessionWithOptions().noInterceptor().open() ) {
				assertThat( child.getInterceptor() ).isSameAs( EmptyInterceptor.INSTANCE );
			}
		}
	}

	/// child calls interceptor() (share the parent's) - the parent has none, so
	/// the child must also have none. It must NOT fall back to the
	/// SessionFactory's configured Interceptor just because sharing "found
	/// nothing" - that was the bug reported by Steve Ebersole on HHH-12168.
	@ParameterizedTest(name = "{0}")
	@EnumSource(value = EndState.class, names = { "GLOBAL", "SCOPED_CLASS" })
	void parentHasNoInterceptor_childSharesParent_getsNone(EndState state) {
		try ( var parent = openParent( state, ParentAction.NO_INTERCEPTOR, true ) ) {
			assertThat( parent.getInterceptor() ).isSameAs( EmptyInterceptor.INSTANCE );
			try ( var child = (SharedSessionContractImplementor) parent.sessionWithOptions().interceptor().open() ) {
				assertThat( child.getInterceptor() ).isSameAs( EmptyInterceptor.INSTANCE );
			}
		}
	}

	/// child calls interceptor(Interceptor) - always gets that exact instance,
	/// regardless of the parent's (or the SessionFactory's) state.
	@ParameterizedTest(name = "{0}")
	@EnumSource(value = EndState.class, names = { "GLOBAL", "SCOPED_CLASS" })
	void parentHasNoInterceptor_childExplicitInstance_getsThatInstance(EndState state) {
		final var explicit = new MarkerInterceptor();
		try ( var parent = openParent( state, ParentAction.NO_INTERCEPTOR, true ) ) {
			try ( var child = (SharedSessionContractImplementor) parent.sessionWithOptions().interceptor( explicit ).open() ) {
				assertThat( child.getInterceptor() ).isSameAs( explicit );
			}
		}
	}

	/// child calls noSessionInterceptorCreation() - this is not a sharing
	/// operation, so the parent's empty Interceptor is irrelevant. The child
	/// resolves directly against the SessionFactory's ConfiguredInterceptor:
	/// a GLOBAL Interceptor still applies, while SCOPED creation is suppressed.
	@ParameterizedTest(name = "{0}")
	@EnumSource(value = EndState.class, names = { "GLOBAL", "SCOPED_CLASS" })
	void parentHasNoInterceptor_childNoSessionInterceptorCreation_usesConfiguredInterceptorIfNonScoped(EndState state) {
		final var sessionFactory = factoryFor( state );
		try ( var parent = openParent( state, ParentAction.NO_INTERCEPTOR, true ) ) {
			try ( var child = (SharedSessionContractImplementor)
					parent.sessionWithOptions().noSessionInterceptorCreation().open() ) {
				switch ( state ) {
					case GLOBAL -> assertThat( child.getInterceptor() )
							.isSameAs( sessionFactory.getInterceptorStrategy().getFactoryInterceptor() );
					case SCOPED_CLASS -> assertThat( child.getInterceptor() ).isSameAs( EmptyInterceptor.INSTANCE );
					default -> throw new IllegalStateException( "Unexpected state: " + state );
				}
			}
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
