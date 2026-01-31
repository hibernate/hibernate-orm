/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Temporal;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@DomainModel(annotatedClasses =
		{TemporalEntityPartitionedTest.TemporalEntity3.class,
		TemporalEntityPartitionedTest.TemporalChild3.class})
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"))
class TemporalEntityPartitionedTest {

	@Test void test(SessionFactoryScope scope) throws InterruptedException {
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity3 entity = new TemporalEntity3();
					entity.id = 1L;
					entity.text = "hello";
					entity.strings.add( "x" );
					session.persist( entity );
					TemporalChild3 child = new TemporalChild3();
					child.id = 1L;
					child.text = "world";
					child.parent = entity;
					session.persist( child );
				}
		);
		var instant = getInstant( scope );
		Thread.sleep( 250 );
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity3 entity = session.find( TemporalEntity3.class, 1L );
					entity.text = "goodbye";
					entity.strings.add( "y" );
					entity.children.get(0).text = "world!";
					TemporalChild3 friend = new TemporalChild3();
					friend.id = 5L;
					friend.text = "friend";
					session.persist( friend );
					entity.children.get(0).friends.add( friend );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity3 entity = session.find( TemporalEntity3.class, 1L );
					assertEquals( "goodbye", entity.text );
					assertEquals( 1, entity.children.size() );
					assertEquals( "world!", entity.children.get(0).text );
					assertEquals( 2, entity.strings.size() );
					assertEquals( 1, entity.children.get(0).friends.size() );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity3 entity =
							session.createSelectionQuery( "from TemporalEntity3 where id=1", TemporalEntity3.class )
									.getSingleResult();
					assertEquals( "goodbye", entity.text );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity3 entity =
							session.createSelectionQuery( "from TemporalEntity3 p left join fetch p.children c where p.id=1", TemporalEntity3.class )
									.getSingleResult();
					assertTrue( Hibernate.isInitialized(entity.children) );
					assertEquals( "goodbye", entity.text );
					assertEquals( 1, entity.children.size() );
					assertEquals( "world!", entity.children.get(0).text );
					var friends =
							session.createSelectionQuery( "select f from TemporalEntity3 p join p.children c join c.friends f where p.id=1", TemporalChild3.class )
									.getResultCount();
					assertEquals( 1, friends );
				}
		);
		try (var session = scope.getSessionFactory().withOptions().asOf(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity3 entity = session.find( TemporalEntity3.class, 1L );
				assertEquals( "hello", entity.text );
				assertEquals( 1, entity.children.size() );
				assertEquals( "world", entity.children.get(0).text );
				assertEquals( 1, entity.strings.size() );
				assertEquals( 0, entity.children.get(0).friends.size() );
			} );
		}
		try (var session = scope.getSessionFactory().withOptions().asOf(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity3 entity =
						session.createSelectionQuery( "from TemporalEntity3 where id=1", TemporalEntity3.class )
								.getSingleResult();
				assertEquals( "hello", entity.text );
			} );
		}
		try (var session = scope.getSessionFactory().withOptions().asOf(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity3 entity =
						session.createSelectionQuery( "from TemporalEntity3 p left join fetch p.children c where p.id=1", TemporalEntity3.class )
								.getSingleResult();
				assertTrue( Hibernate.isInitialized(entity.children) );
				assertEquals( "hello", entity.text );
				assertEquals( 1, entity.children.size() );
				assertEquals( "world", entity.children.get(0).text );
				var friends =
						session.createSelectionQuery( "select f from TemporalEntity3 p join p.children c join c.friends f where p.id=1", TemporalChild3.class )
								.getResultCount();
				assertEquals( 0, friends );
			} );
		}
		var nextInstant = getInstant( scope );
		Thread.sleep( 250 );
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity3 entity = session.find( TemporalEntity3.class, 1L );
					entity.strings.remove( "x" );
					entity.strings.add( "z" );
					entity.children.get(0).friends.clear();
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity3 entity = session.find( TemporalEntity3.class, 1L );
					assertEquals( Set.of("y", "z"), entity.strings );
					assertEquals( 0, entity.children.get(0).friends.size() );
				}
		);
		try (var session = scope.getSessionFactory().withOptions().asOf(instant).open()) {
			scope.getSessionFactory().inTransaction(
					tx -> {
						TemporalEntity3 entity = session.find( TemporalEntity3.class, 1L );
						assertEquals( Set.of( "x" ), entity.strings );
						assertEquals( 0, entity.children.get( 0 ).friends.size() );
					}
			);
		}
		try (var session = scope.getSessionFactory().withOptions().asOf(nextInstant).open()) {
			scope.getSessionFactory().inTransaction(
					tx -> {
						TemporalEntity3 entity = session.find( TemporalEntity3.class, 1L );
						assertEquals( Set.of( "x", "y" ), entity.strings );
						assertEquals( 1, entity.children.get( 0 ).friends.size() );
					}
			);
		}
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity3 entity = session.find( TemporalEntity3.class, 1L );
					session.remove( entity );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity3 entity = session.find( TemporalEntity3.class, 1L );
					assertNull( entity );
				}
		);
		try (var session = scope.getSessionFactory().withOptions().asOf(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity3 entity = session.find( TemporalEntity3.class, 1L );
				assertEquals( "hello", entity.text );
			} );
		}
	}

	@Test void testStateless(SessionFactoryScope scope) throws InterruptedException {
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity3 entity = new TemporalEntity3();
					entity.id = 2L;
					entity.text = "hello";
					session.insert( entity );
				}
		);
		var instant = getInstant( scope );
		Thread.sleep( 250 );
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity3 entity = session.get( TemporalEntity3.class, 2L );
					entity.text = "goodbye";
					session.update( entity );
				}
		);
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity3 entity = session.get( TemporalEntity3.class, 2L );
					assertEquals( "goodbye", entity.text );
					entity =
							session.createSelectionQuery( "from TemporalEntity3 where id=2", TemporalEntity3.class )
									.getSingleResult();
					assertEquals( "goodbye", entity.text );
				}
		);
		try (var session = scope.getSessionFactory().withStatelessOptions().asOf(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity3 entity = session.get( TemporalEntity3.class, 2L );
				assertEquals( "hello", entity.text );
				entity =
						session.createSelectionQuery( "from TemporalEntity3 where id=2", TemporalEntity3.class )
								.getSingleResult();
				assertEquals( "hello", entity.text );
			} );
		}
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity3 entity = session.get( TemporalEntity3.class, 2L );
					session.delete( entity );
				}
		);
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity3 entity = session.get( TemporalEntity3.class, 2L );
					assertNull( entity );
				}
		);
		try (var session = scope.getSessionFactory().withStatelessOptions().asOf(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity3 entity = session.get( TemporalEntity3.class, 2L );
				assertEquals( "hello", entity.text );
			} );
		}
	}

	private static Instant getInstant(SessionFactoryScope scope) {
		return scope.getSessionFactory().fromSession(
				s -> s.createSelectionQuery( "select instant", Instant.class ).getSingleResult() );
	}

	@Temporal(rowStart = "effective_from", rowEnd = "effective_to")
	@Temporal.HistoryPartitioning
	@Entity(name = "TemporalEntity3")
	static class TemporalEntity3 {
		@Id
		long id;
		@Version
		int version;
		String text;
		@OneToMany(mappedBy = "parent")
		List<TemporalChild3> children = new ArrayList<>();
		@Temporal @Temporal.HistoryPartitioning
		@ElementCollection
		Set<String> strings = new HashSet<>();
	}

	@Temporal(rowStart = "effective_from", rowEnd = "effective_to")
	@Temporal.HistoryPartitioning
	@Entity(name = "TemporalChild3")
	static class TemporalChild3 {
		@Id
		long id;
		@Version
		int version;
		String text;
		@ManyToOne @JoinColumn
		TemporalEntity3 parent;

		@Temporal @Temporal.HistoryPartitioning
		@ManyToMany
		Set<TemporalChild3> friends = new HashSet<>();
	}
}
