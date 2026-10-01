/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.creation.CommonSharedBuilder;
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
 * Baseline (pre-CDI) {@code [CommonSharedBuilder]} cross-check
 * every {@link CommonSharedBuilder} interceptor action on a child session, crossed against
 * every SessionFactory-level interceptor end-state, for both stateful and stateless
 * children. Per the approved scope cut, the parent session is always stateful -
 * {@link org.hibernate.SharedSessionBuilder#interceptor()} and
 * {@link org.hibernate.SharedStatelessSessionBuilder#interceptor()} were both confirmed
 * (by reading source) to resolve identically off the parent's already-resolved
 * {@code Interceptor}, regardless of the parent's own stateful/stateless kind.
 */
@JiraKey("HHH-12168")
class CommonSharedBuilderInterceptorBaselineTest {

	enum EndState { NONE, GLOBAL, SCOPED }

	enum ChildAction { DEFAULT, EXPLICIT_INSTANCE, NO_INTERCEPTOR, NO_SESSION_INTERCEPTOR_CREATION, SHARE_PARENT }

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

	static Stream<Arguments> matrixCells() {
		final List<Arguments> cells = new ArrayList<>();
		for ( var state : EndState.values() ) {
			for ( var action : ChildAction.values() ) {
				for ( var statefulChild : new boolean[] { true, false } ) {
					cells.add( Arguments.of( state, action, statefulChild ) );
				}
			}
		}
		return cells.stream();
	}

	@ParameterizedTest(name = "[{index}] {0} x {1} x statefulChild={2}")
	@MethodSource("matrixCells")
	void crossProduct(EndState state, ChildAction action, boolean statefulChild) {
		final var sessionFactory = factoryFor( state );
		// the parent always carries its own distinct, explicit interceptor, so that
		// SHARE_PARENT is unambiguously distinguishable from whatever the SF state alone
		// would otherwise resolve to.
		final var parentMarker = new MarkerInterceptor();
		final var childMarker = new MarkerInterceptor();

		try ( Session parent = sessionFactory.withOptions().interceptor( parentMarker ).openSession() ) {
			final CommonSharedBuilder childBuilder =
					statefulChild ? parent.sessionWithOptions() : parent.statelessWithOptions();

			try ( var child = openChildWithAction( childBuilder, action, childMarker ) ) {
				final var resolved = child.getInterceptor();
				switch ( action ) {
					case DEFAULT -> assertDefaultResolution( state, sessionFactory, resolved );
					case EXPLICIT_INSTANCE -> assertThat( resolved ).isSameAs( childMarker );
					case NO_INTERCEPTOR -> assertThat( resolved ).isSameAs( EmptyInterceptor.INSTANCE );
					case NO_SESSION_INTERCEPTOR_CREATION -> assertNoSessionInterceptorCreationResolution( state, sessionFactory, resolved );
					case SHARE_PARENT -> assertThat( resolved ).isSameAs( parentMarker );
				}
			}
		}
	}

	private void assertDefaultResolution(EndState state, SessionFactoryImplementor sessionFactory, Interceptor resolved) {
		switch ( state ) {
			case NONE -> assertThat( resolved ).isSameAs( EmptyInterceptor.INSTANCE );
			case GLOBAL -> assertThat( resolved ).isSameAs( sessionFactory.getSessionFactoryOptions().getInterceptor() );
			case SCOPED -> assertThat( resolved ).isInstanceOf( StatefulInterceptor.class );
		}
	}

	private void assertNoSessionInterceptorCreationResolution(
			EndState state, SessionFactoryImplementor sessionFactory, Interceptor resolved) {
		switch ( state ) {
			case NONE -> assertThat( resolved ).isSameAs( EmptyInterceptor.INSTANCE );
			case GLOBAL -> assertThat( resolved ).isSameAs( sessionFactory.getSessionFactoryOptions().getInterceptor() );
			case SCOPED -> assertThat( resolved ).isSameAs( EmptyInterceptor.INSTANCE );
		}
	}

	private static SharedSessionContractImplementor openChildWithAction(
			CommonSharedBuilder builder, ChildAction action, Interceptor childMarker) {
		switch ( action ) {
			case DEFAULT -> {}
			case EXPLICIT_INSTANCE -> builder.interceptor( childMarker );
			case NO_INTERCEPTOR -> builder.noInterceptor();
			case NO_SESSION_INTERCEPTOR_CREATION -> builder.noSessionInterceptorCreation();
			case SHARE_PARENT -> builder.interceptor();
		}
		return (SharedSessionContractImplementor) builder.open();
	}

	// ---------------------------------------------------------------
	// Order test: SHARE_PARENT (bare interceptor()) sets the child builder's interceptor
	// field directly, which CommonOptions#resolveInterceptor checks before ever consulting
	// the noSessionInterceptorCreation gate - so combining the two should be order-independent.
	// ---------------------------------------------------------------

	@Test
	void shareParentAndNoSessionInterceptorCreation_Stateful() {
		assertShareParentOrderIndependence( true );
	}

	@Test
	void shareParentAndNoSessionInterceptorCreation_Stateless() {
		assertShareParentOrderIndependence( false );
	}

	private void assertShareParentOrderIndependence(boolean statefulChild) {
		final var parentMarker = new MarkerInterceptor();

		try ( Session parent = noneFactory.withOptions().interceptor( parentMarker ).openSession() ) {
			final CommonSharedBuilder builder1 =
					statefulChild ? parent.sessionWithOptions() : parent.statelessWithOptions();
			builder1.interceptor().noSessionInterceptorCreation();
			try ( var child1 = (SharedSessionContractImplementor) builder1.open() ) {
				assertThat( child1.getInterceptor() ).isSameAs( parentMarker );
			}

			final CommonSharedBuilder builder2 =
					statefulChild ? parent.sessionWithOptions() : parent.statelessWithOptions();
			builder2.noSessionInterceptorCreation().interceptor();
			try ( var child2 = (SharedSessionContractImplementor) builder2.open() ) {
				assertThat( child2.getInterceptor() ).isSameAs( parentMarker );
			}
		}
	}

	public static class MarkerInterceptor implements Interceptor {
	}
}
