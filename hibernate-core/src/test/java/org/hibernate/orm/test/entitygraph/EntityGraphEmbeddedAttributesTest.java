/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.Session;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_FETCH_GRAPH;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOAD_GRAPH;

@DomainModel( annotatedClasses = {
		EntityGraphEmbeddedAttributesTest.TrackedProduct.class,
		EntityGraphEmbeddedAttributesTest.Tracking.class,
		EntityGraphEmbeddedAttributesTest.UserForTracking.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-18391" )
public class EntityGraphEmbeddedAttributesTest {
	private static final int TRACKED_PRODUCT_ID = 1;

	@Test
	void testFetchGraphFind(SessionFactoryScope scope) {
		executeTest( scope, HINT_SPEC_FETCH_GRAPH, true );
	}

	@Test
	void testFetchGraphQuery(SessionFactoryScope scope) {
		executeTest( scope, HINT_SPEC_FETCH_GRAPH, false );
	}

	@Test
	void testLoadGraphFind(SessionFactoryScope scope) {
		executeTest( scope, HINT_SPEC_LOAD_GRAPH, true );
	}

	@Test
	void testLoadGraphQuery(SessionFactoryScope scope) {
		executeTest( scope, HINT_SPEC_LOAD_GRAPH, false );
	}

	static void executeTest(SessionFactoryScope scope, String hint, boolean find) {
		scope.inTransaction( session -> {
			final EntityGraph<TrackedProduct> graph = createGraph( session );
			final TrackedProduct product = find ? session.find(
					TrackedProduct.class,
					TRACKED_PRODUCT_ID,
					Map.of( hint, graph )
			) : session.createQuery(
					"from TrackedProduct where id = :id",
					TrackedProduct.class
			).setParameter( "id", TRACKED_PRODUCT_ID ).setHint( hint, graph ).getSingleResult();
			assertThat( Hibernate.isInitialized( product.tracking.creator ) ).isTrue();
			assertThat( Hibernate.isInitialized( product.tracking.modifier ) ).isFalse();
		} );
	}

	static EntityGraph<TrackedProduct> createGraph(Session session) {
		final EntityGraph<TrackedProduct> graph = session.createEntityGraph( TrackedProduct.class );
		graph.addSubgraph( "tracking" ).addAttributeNodes( "creator" );
		return graph;
	}

	@BeforeAll
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final UserForTracking creator = new UserForTracking( 1, "foo" );
			session.persist( creator );
			final UserForTracking modifier = new UserForTracking( 2, "bar" );
			session.persist( modifier );
			final TrackedProduct product = new TrackedProduct( TRACKED_PRODUCT_ID, new Tracking( creator, modifier ) );
			session.persist( product );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.getSessionFactory().getSchemaManager().truncateMappedObjects() );
	}

	@Entity( name = "TrackedProduct" )
	static class TrackedProduct {
		@Id
		private Integer id;

		@Embedded
		private Tracking tracking;

		public TrackedProduct() {
		}

		public TrackedProduct(Integer id, Tracking tracking) {
			this.id = id;
			this.tracking = tracking;
		}
	}

	@Embeddable
	static class Tracking {
		@ManyToOne( fetch = FetchType.LAZY )
		private UserForTracking creator;

		@ManyToOne( fetch = FetchType.LAZY )
		private UserForTracking modifier;

		public Tracking() {
		}

		public Tracking(UserForTracking creator, UserForTracking modifier) {
			this.creator = creator;
			this.modifier = modifier;
		}
	}

	@Entity( name = "UserForTracking" )
	static class UserForTracking {
		@Id
		private Integer id;

		private String login;

		public UserForTracking() {
		}

		public UserForTracking(Integer id, String login) {
			this.id = id;
			this.login = login;
		}
	}
}
