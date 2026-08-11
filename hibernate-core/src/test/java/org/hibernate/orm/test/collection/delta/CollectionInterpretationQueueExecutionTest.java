/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.delta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.hibernate.cfg.FlushSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Full-stack verification that frozen collection interpretations produce the expected
/// database state through both action-queue implementations.
///
/// @author Steve Ebersole
public class CollectionInterpretationQueueExecutionTest {
	@DomainModel(annotatedClasses = { Owner.class, Child.class })
	@SessionFactory
	@ServiceRegistry(settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy"))
	public static class LegacyQueue {
		@Test
		void executesCreateAndUpdateDeltas(SessionFactoryScope scope) {
			verifyCreateAndUpdate( scope );
		}
	}

	@DomainModel(annotatedClasses = { Owner.class, Child.class })
	@SessionFactory
	@ServiceRegistry(settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "graph"))
	public static class GraphQueue {
		@Test
		void executesCreateAndUpdateDeltas(SessionFactoryScope scope) {
			verifyCreateAndUpdate( scope );
		}

		@Test
		void executesCompactCreateAcrossJdbcBatches(SessionFactoryScope scope) {
			scope.inTransaction( session -> session.persist( newOwner( 100L, 128, 1_000L ) ) );

			scope.inTransaction( session -> {
				final var owner = session.find( Owner.class, 100L );
				assertThat( owner.setValues ).hasSize( 128 );
				assertThat( owner.bagValues ).hasSize( 128 );
				assertThat( owner.listValues ).hasSize( 128 );
				assertThat( owner.mapValues ).hasSize( 128 );
				assertThat( owner.children ).hasSize( 128 );
			} );
		}

		@Test
		void executesCompactBagRecreateAcrossJdbcBatches(SessionFactoryScope scope) {
			scope.inTransaction( session -> session.persist( newOwner( 200L, 128, 2_000L ) ) );

			scope.inTransaction( session -> {
				final var owner = session.find( Owner.class, 200L );
				owner.bagValues.remove( "v64" );
				owner.bagValues.add( "bag-replacement" );
			} );

			scope.inTransaction( session -> {
				final var owner = session.find( Owner.class, 200L );
				assertThat( owner.bagValues )
						.hasSize( 128 )
						.contains( "bag-replacement" )
						.doesNotContain( "v64" );
			} );
		}
	}

	private static void verifyCreateAndUpdate(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( newOwner() ) );

		scope.inTransaction( session -> {
			final var owner = session.find( Owner.class, 1L );
			owner.setValues.remove( "v1" );
			owner.setValues.add( "set-replacement" );
			owner.bagValues.remove( "v1" );
			owner.bagValues.add( "bag-replacement" );
			owner.listValues.set( 1, "list-replacement" );
			owner.mapValues.put( "k1", "map-replacement" );
			owner.children.remove( 1 );
			final var replacement = new Child( 5L, "child-replacement" );
			session.persist( replacement );
			owner.children.add( 1, replacement );
		} );

		scope.inTransaction( session -> {
			final var owner = session.find( Owner.class, 1L );
			assertThat( owner.setValues )
					.containsExactlyInAnyOrder( "v0", "set-replacement", "v2", "v3" );
			assertThat( owner.bagValues )
					.containsExactlyInAnyOrder( "v0", "bag-replacement", "v2", "v3" );
			assertThat( owner.listValues )
					.containsExactly( "v0", "list-replacement", "v2", "v3" );
			assertThat( owner.mapValues ).containsExactlyInAnyOrderEntriesOf( Map.of(
					"k0", "v0",
					"k1", "map-replacement",
					"k2", "v2",
					"k3", "v3"
			) );
			assertThat( owner.children )
					.extracting( child -> child.id )
					.containsExactly( 1L, 5L, 3L, 4L );
		} );
	}

	private static Owner newOwner() {
		return newOwner( 1L, 4, 1L );
	}

	private static Owner newOwner(long id, int size, long firstChildId) {
		final var owner = new Owner();
		owner.id = id;
		for ( int i = 0; i < size; i++ ) {
			final String value = "v" + i;
			owner.setValues.add( value );
			owner.bagValues.add( value );
			owner.listValues.add( value );
			owner.mapValues.put( "k" + i, value );
			owner.children.add( new Child( firstChildId + i, "child-" + i ) );
		}
		return owner;
	}

	@Entity(name = "CollectionDeltaExecutionOwner")
	@Table(name = "collection_delta_execution_owner")
	public static class Owner {
		@Id
		private Long id;

		@ElementCollection
		@CollectionTable(name = "collection_delta_execution_set")
		@Column(name = "set_value")
		private Set<String> setValues = new HashSet<>();

		@ElementCollection
		@CollectionTable(name = "collection_delta_execution_bag")
		@Column(name = "bag_value")
		private List<String> bagValues = new ArrayList<>();

		@ElementCollection
		@CollectionTable(name = "collection_delta_execution_list")
		@OrderColumn(name = "list_position")
		@Column(name = "list_value")
		private List<String> listValues = new ArrayList<>();

		@ElementCollection
		@CollectionTable(name = "collection_delta_execution_map")
		@MapKeyColumn(name = "map_key")
		@Column(name = "map_value")
		private Map<String, String> mapValues = new HashMap<>();

		@OneToMany(cascade = CascadeType.ALL)
		@JoinColumn(name = "owner_id")
		@OrderColumn(name = "child_position")
		private List<Child> children = new ArrayList<>();
	}

	@Entity(name = "CollectionInterpretationChild")
	@Table(name = "collection_interpretation_child")
	public static class Child {
		@Id
		private Long id;

		private String name;

		protected Child() {
		}

		private Child(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
