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
import jakarta.persistence.OrderColumn;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@DomainModel(annotatedClasses =
		{TemporalEntityHistoryTest.TemporalEntity4.class,
		TemporalEntityHistoryTest.TemporalChild4.class})
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"))
class TemporalEntityHistoryTest {

	@Test void test(SessionFactoryScope scope) throws InterruptedException {
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity4 entity = new TemporalEntity4();
					entity.id = 1L;
					entity.text = "hello";
					entity.strings.add( "x" );
					entity.excluded = 1L;
					session.persist( entity );
					TemporalChild4 child = new TemporalChild4();
					child.id = 1L;
					child.text = "world";
					child.parent = entity;
					session.persist( child );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity4 entity = session.find( TemporalEntity4.class, 1L );
					assertEquals( 1L, entity.excluded );
					entity.excluded = 2L;
				}
		);
		Thread.sleep( 250 );
		var instant = getInstant( scope );
		Thread.sleep( 250 );
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity4 entity = session.find( TemporalEntity4.class, 1L );
					assertEquals( 2L, entity.excluded );
					entity.text = "goodbye";
					entity.strings.add( "y" );
					entity.children.get(0).text = "world!";
					entity.excluded = 5L;
					TemporalChild4 friend = new TemporalChild4();
					friend.id = 5L;
					friend.text = "friend";
					session.persist( friend );
					entity.children.get(0).friends.add( friend );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity4 entity = session.find( TemporalEntity4.class, 1L );
					assertEquals( "goodbye", entity.text );
					assertEquals( 5L, entity.excluded );
					assertEquals( 1, entity.children.size() );
					assertEquals( "world!", entity.children.get(0).text );
					assertEquals( 2, entity.strings.size() );
					assertEquals( 1, entity.children.get(0).friends.size() );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity4 entity =
							session.createSelectionQuery( "from TemporalEntity4 where id=1", TemporalEntity4.class )
									.getSingleResult();
					assertEquals( "goodbye", entity.text );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity4 entity =
							session.createSelectionQuery( "from TemporalEntity4 p left join fetch p.children c where p.id=1", TemporalEntity4.class )
									.getSingleResult();
					assertTrue( Hibernate.isInitialized(entity.children) );
					assertEquals( "goodbye", entity.text );
					assertEquals( 1, entity.children.size() );
					assertEquals( "world!", entity.children.get(0).text );
					var friends =
							session.createSelectionQuery( "select f from TemporalEntity4 p join p.children c join c.friends f where p.id=1", TemporalChild4.class )
									.getResultCount();
					assertEquals( 1, friends );
				}
		);
		try (var session = scope.getSessionFactory().withOptions().asOf(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity4 entity = session.find( TemporalEntity4.class, 1L );
				assertEquals( "hello", entity.text );
				assertEquals( 2L, entity.excluded );
				assertEquals( 1, entity.children.size() );
				assertEquals( "world", entity.children.get(0).text );
				assertEquals( 1, entity.strings.size() );
				assertEquals( 0, entity.children.get(0).friends.size() );
			} );
		}
		try (var session = scope.getSessionFactory().withOptions().asOf(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity4 entity =
						session.createSelectionQuery( "from TemporalEntity4 where id=1", TemporalEntity4.class )
								.getSingleResult();
				assertEquals( "hello", entity.text );
			} );
		}
		try (var session = scope.getSessionFactory().withOptions().asOf(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity4 entity =
						session.createSelectionQuery( "from TemporalEntity4 p left join fetch p.children c where p.id=1", TemporalEntity4.class )
								.getSingleResult();
				assertTrue( Hibernate.isInitialized(entity.children) );
				assertEquals( "hello", entity.text );
				assertEquals( 1, entity.children.size() );
				assertEquals( "world", entity.children.get(0).text );
				var friends =
						session.createSelectionQuery( "select f from TemporalEntity4 p join p.children c join c.friends f where p.id=1", TemporalChild4.class )
								.getResultCount();
				assertEquals( 0, friends );
			} );
		}
		Thread.sleep( 250 );
		var nextInstant = getInstant( scope );
		Thread.sleep( 250 );
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity4 entity = session.find( TemporalEntity4.class, 1L );
					entity.strings.remove( "x" );
					entity.strings.add( "z" );
					entity.children.get(0).friends.clear();
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity4 entity = session.find( TemporalEntity4.class, 1L );
					assertEquals( Set.of("y", "z"), entity.strings );
					assertEquals( 0, entity.children.get(0).friends.size() );
				}
		);
		try (var session = scope.getSessionFactory().withOptions().asOf(instant).open()) {
			scope.getSessionFactory().inTransaction(
					tx -> {
						TemporalEntity4 entity = session.find( TemporalEntity4.class, 1L );
						assertEquals( Set.of( "x" ), entity.strings );
						assertEquals( 0, entity.children.get( 0 ).friends.size() );
					}
			);
		}
		try (var session = scope.getSessionFactory().withOptions().asOf(nextInstant).open()) {
			scope.getSessionFactory().inTransaction(
					tx -> {
						TemporalEntity4 entity = session.find( TemporalEntity4.class, 1L );
						assertEquals( Set.of( "x", "y" ), entity.strings );
						assertEquals( 1, entity.children.get( 0 ).friends.size() );
					}
			);
		}
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity4 entity = session.find( TemporalEntity4.class, 1L );
					session.remove( entity.children.get(0) );
					session.remove( entity );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity4 entity = session.find( TemporalEntity4.class, 1L );
					assertNull( entity );
				}
		);
		try (var session = scope.getSessionFactory().withOptions().asOf(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity4 entity = session.find( TemporalEntity4.class, 1L );
				assertEquals( "hello", entity.text );
			} );
		}
	}

	@Test void testStateless(SessionFactoryScope scope) throws InterruptedException {
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity4 entity = new TemporalEntity4();
					entity.id = 2L;
					entity.text = "hello";
					session.insert( entity );
				}
		);
		Thread.sleep( 250 );
		var instant = getInstant( scope );
		Thread.sleep( 250 );
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity4 entity = session.get( TemporalEntity4.class, 2L );
					entity.text = "goodbye";
					session.update( entity );
				}
		);
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity4 entity = session.get( TemporalEntity4.class, 2L );
					assertEquals( "goodbye", entity.text );
					entity =
							session.createSelectionQuery( "from TemporalEntity4 where id=2", TemporalEntity4.class )
									.getSingleResult();
					assertEquals( "goodbye", entity.text );
				}
		);
		try (var session = scope.getSessionFactory().withStatelessOptions().asOf(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity4 entity = session.get( TemporalEntity4.class, 2L );
				assertEquals( "hello", entity.text );
				entity =
						session.createSelectionQuery( "from TemporalEntity4 where id=2", TemporalEntity4.class )
								.getSingleResult();
				assertEquals( "hello", entity.text );
			} );
		}
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity4 entity = session.get( TemporalEntity4.class, 2L );
					session.delete( entity );
				}
		);
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity4 entity = session.get( TemporalEntity4.class, 2L );
					assertNull( entity );
				}
		);
		try (var session = scope.getSessionFactory().withStatelessOptions().asOf(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity4 entity = session.get( TemporalEntity4.class, 2L );
				assertEquals( "hello", entity.text );
			} );
		}
	}


	@Test void testCollections(SessionFactoryScope scope) throws InterruptedException {
		scope.inTransaction( s -> {
			TemporalEntity4 entity = new TemporalEntity4();
			entity.id = 3L;
			entity.text = "hello";
			entity.strings.add( "x" );
			entity.strings.add( "y" );
			entity.strings.add( "z" );
			entity.map.put( "a", "A" );
			entity.map.put( "b", "B" );
			entity.list.add( "M" );
			entity.list.add( "N" );
			s.persist( entity );
		} );
		var instant = getInstant( scope );
		Thread.sleep( 250 );
		scope.inTransaction( s -> {
			TemporalEntity4 entity = s.find( TemporalEntity4.class, 3L);
			assertEquals( Set.of("x", "y", "z"), entity.strings );
			assertEquals( Map.of("a", "A", "b", "B"), entity.map );
			assertEquals( List.of("M", "N"), entity.list );
			entity.strings.add( "w" );
			entity.map.put( "c", "C" );
			entity.list.add( "K" );
		} );
		scope.inTransaction( s -> {
			TemporalEntity4 entity = s.find( TemporalEntity4.class, 3L);
			assertEquals( Set.of("w", "x", "y", "z"), entity.strings );
			assertEquals( Map.of("a", "A", "b", "B", "c", "C"), entity.map );
			assertEquals( List.of("M", "N", "K"), entity.list );
			entity.strings.remove( "x" );
			entity.map.remove( "b" );
			entity.list.remove( "N" );
			entity.list.add( 0, "P" );
		} );
		scope.inTransaction( s -> {
			TemporalEntity4 entity = s.find( TemporalEntity4.class, 3L);
			assertEquals( Set.of("w", "y", "z"), entity.strings );
			assertEquals( Map.of("a", "A", "c", "C"), entity.map );
			assertEquals( List.of("P", "M", "K"), entity.list );
			entity.strings.remove( "z" );
			entity.strings.add( "v" );
			entity.map.put( "c", "CC" );
			entity.map.put( "a", "AA" );
			entity.list.set( 2, "L" );
			entity.list.set( 0, "Q" );
		} );
		scope.inTransaction( s -> {
			TemporalEntity4 entity = s.find( TemporalEntity4.class, 3L);
			assertEquals( Set.of("v", "w", "y"), entity.strings );
			assertEquals( Map.of("a", "AA", "c", "CC"), entity.map );
			assertEquals( List.of("Q", "M", "L"), entity.list );
			entity.strings = null;
			entity.map = null;
			entity.list = null;
		} );
		scope.inTransaction( s -> {
			TemporalEntity4 entity = s.find(TemporalEntity4.class, 3L);
			assertEquals( Set.of(), entity.strings );
			assertEquals( Map.of(), entity.map );
			assertEquals( List.of(), entity.list );
			s.remove( entity );
		} );
		try (var s = scope.getSessionFactory().withOptions().asOf(instant).open()) {
			s.inTransaction( tx -> {
				TemporalEntity4 entity = s.find( TemporalEntity4.class, 3L);
				assertEquals( Set.of("x", "y", "z"), entity.strings );
				assertEquals( Map.of("a", "A", "b", "B"), entity.map );
				assertEquals( List.of("M", "N"), entity.list );
			} );
		}
	}

	@Test void testUpsert(SessionFactoryScope scope) {
		scope.inStatelessTransaction( s -> {
			TemporalEntity4 entity = new TemporalEntity4();
			entity.id = 7L;
			entity.text = "hello";
			entity.strings.add( "x" );
			entity.strings.add( "y" );
			entity.strings.add( "z" );
			entity.map.put( "a", "A" );
			entity.map.put( "b", "B" );
			entity.list.add( "M" );
			entity.list.add( "N" );
			s.upsert( entity );
		} );
		TemporalEntity4 entity1 = scope.getSessionFactory().fromStatelessTransaction( s -> {
			TemporalEntity4 entity = s.get( TemporalEntity4.class, 7L );
			s.fetch( entity.strings );
			s.fetch( entity.map );
			s.fetch( entity.list );
			assertEquals( "hello", entity.text );
			assertEquals( Set.of( "x", "y", "z" ), entity.strings );
			assertEquals( Map.of( "a", "A", "b", "B" ), entity.map );
			assertEquals( List.of( "M", "N" ), entity.list );
			return entity;
		} );
		entity1.text = "goodbye";
		entity1.strings.add( "w" );
		entity1.map.put( "c", "C" );
		scope.inStatelessTransaction( s -> {
			s.upsert( entity1 );
		} );
		scope.inStatelessTransaction( s -> {
			TemporalEntity4 entity = s.get( TemporalEntity4.class, 7L );
			s.fetch( entity.strings );
			s.fetch( entity.map );
			s.fetch( entity.list );
			assertEquals( "goodbye", entity.text );
			assertEquals( Set.of( "w", "x", "y", "z" ), entity.strings );
			assertEquals( Map.of( "a", "A", "b", "B", "c", "C" ), entity.map );
			assertEquals( List.of( "M", "N" ), entity.list );
		} );
	}

	private static Instant getInstant(SessionFactoryScope scope) {
		return scope.getSessionFactory().fromSession(
				s -> s.createSelectionQuery( "select instant", Instant.class ).getSingleResult() );
	}

	@Temporal(rowStart = "effective_from", rowEnd = "effective_to")
	@Entity(name = "TemporalEntity4")
	static class TemporalEntity4 {
		@Id
		long id;
		@Version
		int version;
		String text;
		@OneToMany(mappedBy = "parent")
		List<TemporalChild4> children = new ArrayList<>();
		@Temporal
		@ElementCollection
		Set<String> strings = new HashSet<>();
		@Temporal
		@ElementCollection
		Map<String,String> map = new HashMap<>();
		@Temporal
		@ElementCollection
		@OrderColumn
		List<String> list = new ArrayList<>();
		@Temporal.Excluded
		long excluded;
	}

	@Temporal(rowStart = "effective_from", rowEnd = "effective_to")
	@Entity(name = "TemporalChild4")
	static class TemporalChild4 {
		@Id
		long id;
		@Version
		int version;
		String text;
		@ManyToOne @JoinColumn
		TemporalEntity4 parent;

		@Temporal @ManyToMany
		Set<TemporalChild4> friends = new HashSet<>();
	}
}
