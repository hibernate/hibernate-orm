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
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.VersionMatchMode;
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
		{TemporalEntityTest.TemporalEntity1.class,
		TemporalEntityTest.TemporalChild1.class})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"))
@SkipForDialect(dialectClass = MariaDBDialect.class, majorVersion = 10, minorVersion = 11, versionMatchMode = VersionMatchMode.OLDER, reason = "See https://jira.mariadb.org/browse/MDEV-39230")
class TemporalEntityTest {

	@Test void test(SessionFactoryScope scope) throws InterruptedException {
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity1 entity = new TemporalEntity1();
					entity.id = 1L;
					entity.text = "hello";
					entity.strings.add( "x" );
					entity.excluded = 1L;
					session.persist( entity );
					TemporalChild1 child = new TemporalChild1();
					child.id = 1L;
					child.text = "world";
					child.parent = entity;
					session.persist( child );
				}
		);
		awaitOracleClockTick();
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity1 entity = session.find( TemporalEntity1.class, 1L );
					assertEquals( 1L, entity.excluded );
					entity.excluded = 2L;
				}
		);
		var instant = Instant.now();
		awaitOracleClockTick();
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity1 entity = session.find( TemporalEntity1.class, 1L );
					assertEquals( 2L, entity.excluded );
					entity.text = "goodbye";
					entity.excluded = 5L;
					entity.strings.add( "y" );
					entity.children.get(0).text = "world!";
					TemporalChild1 friend = new TemporalChild1();
					friend.id = 5L;
					friend.text = "friend";
					session.persist( friend );
					entity.children.get(0).friends.add( friend );
				}
		);
		awaitOracleClockTick();
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity1 entity = session.find( TemporalEntity1.class, 1L );
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
					TemporalEntity1 entity =
							session.createSelectionQuery( "from TemporalEntity1 where id=1", TemporalEntity1.class )
									.getSingleResult();
					assertEquals( "goodbye", entity.text );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity1 entity =
							session.createSelectionQuery( "from TemporalEntity1 p left join fetch p.children c where p.id=1", TemporalEntity1.class )
									.getSingleResult();
					assertTrue( Hibernate.isInitialized(entity.children) );
					assertEquals( "goodbye", entity.text );
					assertEquals( 1, entity.children.size() );
					assertEquals( "world!", entity.children.get(0).text );
					var friends =
							session.createSelectionQuery( "select f from TemporalEntity1 p join p.children c join c.friends f where p.id=1", TemporalChild1.class )
									.getResultCount();
					assertEquals( 1, friends );
				}
		);
		try (var session = scope.getSessionFactory().withOptions().asOf(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity1 entity = session.find( TemporalEntity1.class, 1L );
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
				TemporalEntity1 entity =
						session.createSelectionQuery( "from TemporalEntity1 where id=1", TemporalEntity1.class )
								.getSingleResult();
				assertEquals( "hello", entity.text );
			} );
		}
		try (var session = scope.getSessionFactory().withOptions().asOf(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity1 entity =
						session.createSelectionQuery( "from TemporalEntity1 p left join fetch p.children c where p.id=1", TemporalEntity1.class )
								.getSingleResult();
				assertTrue( Hibernate.isInitialized(entity.children) );
				assertEquals( "hello", entity.text );
				assertEquals( 1, entity.children.size() );
				assertEquals( "world", entity.children.get(0).text );
				var friends =
						session.createSelectionQuery( "select f from TemporalEntity1 p join p.children c join c.friends f where p.id=1", TemporalChild1.class )
								.getResultCount();
				assertEquals( 0, friends );
			} );
		}
		var nextInstant = Instant.now();
		awaitOracleClockTick();
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity1 entity = session.find( TemporalEntity1.class, 1L );
					entity.strings.remove( "x" );
					entity.strings.add( "z" );
					entity.children.get(0).friends.clear();
				}
		);
		awaitOracleClockTick();
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity1 entity = session.find( TemporalEntity1.class, 1L );
					assertEquals( Set.of("y", "z"), entity.strings );
					assertEquals( 0, entity.children.get(0).friends.size() );
				}
		);
		try (var session = scope.getSessionFactory().withOptions().asOf(instant).open()) {
			scope.getSessionFactory().inTransaction(
					tx -> {
						TemporalEntity1 entity = session.find( TemporalEntity1.class, 1L );
						assertEquals( Set.of( "x" ), entity.strings );
						assertEquals( 0, entity.children.get( 0 ).friends.size() );
					}
			);
		}
		try (var session = scope.getSessionFactory().withOptions().asOf(nextInstant).open()) {
			scope.getSessionFactory().inTransaction(
					tx -> {
						TemporalEntity1 entity = session.find( TemporalEntity1.class, 1L );
						assertEquals( Set.of( "x", "y" ), entity.strings );
						assertEquals( 1, entity.children.get( 0 ).friends.size() );
					}
			);
		}
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity1 entity = session.find( TemporalEntity1.class, 1L );
					session.remove( entity );
				}
		);
		awaitOracleClockTick();
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity1 entity = session.find( TemporalEntity1.class, 1L );
					assertNull( entity );
				}
		);
		try (var session = scope.getSessionFactory().withOptions().asOf(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity1 entity = session.find( TemporalEntity1.class, 1L );
				assertEquals( "hello", entity.text );
			} );
		}
	}

	@Test void testStateless(SessionFactoryScope scope) throws InterruptedException {
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity1 entity = new TemporalEntity1();
					entity.id = 2L;
					entity.text = "hello";
					session.insert( entity );
				}
		);
		var instant = Instant.now();
		awaitOracleClockTick();
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity1 entity = session.get( TemporalEntity1.class, 2L );
					entity.text = "goodbye";
					session.update( entity );
				}
		);
		awaitOracleClockTick();
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity1 entity = session.get( TemporalEntity1.class, 2L );
					assertEquals( "goodbye", entity.text );
					entity =
							session.createSelectionQuery( "from TemporalEntity1 where id=2", TemporalEntity1.class )
									.getSingleResult();
					assertEquals( "goodbye", entity.text );
				}
		);
		try (var session = scope.getSessionFactory().withStatelessOptions().asOf(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity1 entity = session.get( TemporalEntity1.class, 2L );
				assertEquals( "hello", entity.text );
				entity =
						session.createSelectionQuery( "from TemporalEntity1 where id=2", TemporalEntity1.class )
								.getSingleResult();
				assertEquals( "hello", entity.text );
			} );
		}
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity1 entity = session.get( TemporalEntity1.class, 2L );
					session.delete( entity );
				}
		);
		awaitOracleClockTick();
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity1 entity = session.get( TemporalEntity1.class, 2L );
					assertNull( entity );
				}
		);
		try (var session = scope.getSessionFactory().withStatelessOptions().asOf(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity1 entity = session.get( TemporalEntity1.class, 2L );
				assertEquals( "hello", entity.text );
			} );
		}
	}

	@Test void testCollections(SessionFactoryScope scope) throws InterruptedException {
		scope.inTransaction( s -> {
			TemporalEntity1 entity = new TemporalEntity1();
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
		var instant = Instant.now();
		awaitOracleClockTick();
		scope.inTransaction( s -> {
			TemporalEntity1 entity = s.find(TemporalEntity1.class, 3L);
			assertEquals( Set.of("x", "y", "z"), entity.strings );
			assertEquals( Map.of("a", "A", "b", "B"), entity.map );
			assertEquals( List.of("M", "N"), entity.list );
			entity.strings.add( "w" );
			entity.map.put( "c", "C" );
			entity.list.add( "K" );
		} );
		awaitOracleClockTick();
		scope.inTransaction( s -> {
			TemporalEntity1 entity = s.find(TemporalEntity1.class, 3L);
			assertEquals( Set.of("w", "x", "y", "z"), entity.strings );
			assertEquals( Map.of("a", "A", "b", "B", "c", "C"), entity.map );
			assertEquals( List.of("M", "N", "K"), entity.list );
			entity.strings.remove( "x" );
			entity.map.remove( "b" );
			entity.list.remove( "N" );
			entity.list.add( 0, "P" );
		} );
		awaitOracleClockTick();
		scope.inTransaction( s -> {
			TemporalEntity1 entity = s.find(TemporalEntity1.class, 3L);
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
		awaitOracleClockTick();
		scope.inTransaction( s -> {
			TemporalEntity1 entity = s.find(TemporalEntity1.class, 3L);
			assertEquals( Set.of("v", "w", "y"), entity.strings );
			assertEquals( Map.of("a", "AA", "c", "CC"), entity.map );
			assertEquals( List.of("Q", "M", "L"), entity.list );
			entity.strings = null;
			entity.map = null;
			entity.list = null;
		} );
		awaitOracleClockTick();
		scope.inTransaction( s -> {
			TemporalEntity1 entity = s.find(TemporalEntity1.class, 3L);
			assertEquals( Set.of(), entity.strings );
			assertEquals( Map.of(), entity.map );
			assertEquals( List.of(), entity.list );
			s.remove( entity );
		} );
		awaitOracleClockTick();
		try (var s = scope.getSessionFactory().withOptions().asOf(instant).open()) {
			s.inTransaction( tx -> {
				TemporalEntity1 entity = s.find(TemporalEntity1.class, 3L);
				assertEquals( Set.of("x", "y", "z"), entity.strings );
				assertEquals( Map.of("a", "A", "b", "B"), entity.map );
				assertEquals( List.of("M", "N"), entity.list );
			} );
		}
	}

	@Test void testUpsert(SessionFactoryScope scope) throws InterruptedException {
		scope.inStatelessTransaction( s -> {
			TemporalEntity1 entity = new TemporalEntity1();
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
		awaitOracleClockTick();
		TemporalEntity1 entity1 = scope.getSessionFactory().fromStatelessTransaction( s -> {
			TemporalEntity1 entity = s.get( TemporalEntity1.class, 7L );
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
		awaitOracleClockTick();
		scope.inStatelessTransaction( s -> {
			TemporalEntity1 entity = s.get( TemporalEntity1.class, 7L );
			s.fetch( entity.strings );
			s.fetch( entity.map );
			s.fetch( entity.list );
			assertEquals( "goodbye", entity.text );
			assertEquals( Set.of( "w", "x", "y", "z" ), entity.strings );
			assertEquals( Map.of( "a", "A", "b", "B", "c", "C" ), entity.map );
			assertEquals( List.of( "M", "N" ), entity.list );
		} );
	}

	private static void awaitOracleClockTick() throws InterruptedException {
		// Since the nanosecond resolution clock on the database might be slightly off compared to the JVM clock,
		// wait some time after doing an insert/update to allow a following "as of period for system_time current_timestamp"
		// query to actually see the changes
		Thread.sleep( 100 );
	}

	@Temporal(rowStart = "effective_from", rowEnd = "effective_to")
	@Entity(name = "TemporalEntity1")
	static class TemporalEntity1 {
		@Id
		long id;
		@Version
		int version;
		String text;
		@OneToMany(mappedBy = "parent")
		List<TemporalChild1> children = new ArrayList<>();
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
	@Entity(name = "TemporalChild1")
	static class TemporalChild1 {
		@Id
		long id;
		@Version
		int version;
		String text;
		@ManyToOne @JoinColumn
		TemporalEntity1 parent;

		@Temporal @ManyToMany
		Set<TemporalChild1> friends = new HashSet<>();
	}
}
