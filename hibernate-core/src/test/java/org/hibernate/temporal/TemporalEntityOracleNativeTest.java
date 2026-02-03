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
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Disabled;
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
		{TemporalEntityOracleNativeTest.TemporalEntity5.class,
		TemporalEntityOracleNativeTest.TemporalChild5.class})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.TEMPORAL_TABLE_STRATEGY, value = "NATIVE"))
@RequiresDialect(OracleDialect.class)
@Disabled
class TemporalEntityOracleNativeTest {

	@Test void test(SessionFactoryScope scope) throws InterruptedException {
		pause( scope );
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity5 entity = new TemporalEntity5();
					entity.id = 1L;
					entity.text = "hello";
					entity.strings.add( "x" );
					session.persist( entity );
					TemporalChild5 child = new TemporalChild5();
					child.id = 1L;
					child.text = "world";
					child.parent = entity;
					session.persist( child );
				}
		);
		pause( scope );
		var instant = getInstant( scope );
		pause( scope );
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity5 entity = session.find( TemporalEntity5.class, 1L );
					entity.text = "goodbye";
					entity.strings.add( "y" );
					entity.children.get(0).text = "world!";
					TemporalChild5 friend = new TemporalChild5();
					friend.id = 5L;
					friend.text = "friend";
					session.persist( friend );
					entity.children.get(0).friends.add( friend );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity5 entity = session.find( TemporalEntity5.class, 1L );
					assertEquals( "goodbye", entity.text );
					assertEquals( 1, entity.children.size() );
					assertEquals( "world!", entity.children.get(0).text );
					assertEquals( 2, entity.strings.size() );
					assertEquals( 1, entity.children.get(0).friends.size() );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity5 entity =
							session.createSelectionQuery( "from TemporalEntity5 where id=1", TemporalEntity5.class )
									.getSingleResult();
					assertEquals( "goodbye", entity.text );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity5 entity =
							session.createSelectionQuery( "from TemporalEntity5 p left join fetch p.children c where p.id=1", TemporalEntity5.class )
									.getSingleResult();
					assertTrue( Hibernate.isInitialized(entity.children) );
					assertEquals( "goodbye", entity.text );
					assertEquals( 1, entity.children.size() );
					assertEquals( "world!", entity.children.get(0).text );
					var friends =
							session.createSelectionQuery( "select f from TemporalEntity5 p join p.children c join c.friends f where p.id=1", TemporalChild5.class )
									.getResultCount();
					assertEquals( 1, friends );
				}
		);
		try (var session = scope.getSessionFactory().withOptions().asOf(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity5 entity = session.find( TemporalEntity5.class, 1L );
				assertEquals( "hello", entity.text );
				assertEquals( 1, entity.children.size() );
				assertEquals( "world", entity.children.get(0).text );
				assertEquals( 1, entity.strings.size() );
				assertEquals( 0, entity.children.get(0).friends.size() );
			} );
		}
		try (var session = scope.getSessionFactory().withOptions().asOf(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity5 entity =
						session.createSelectionQuery( "from TemporalEntity5 where id=1", TemporalEntity5.class )
								.getSingleResult();
				assertEquals( "hello", entity.text );
			} );
		}
		try (var session = scope.getSessionFactory().withOptions().asOf(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity5 entity =
						session.createSelectionQuery( "from TemporalEntity5 p left join fetch p.children c where p.id=1", TemporalEntity5.class )
								.getSingleResult();
				assertTrue( Hibernate.isInitialized(entity.children) );
				assertEquals( "hello", entity.text );
				assertEquals( 1, entity.children.size() );
				assertEquals( "world", entity.children.get(0).text );
				var friends =
						session.createSelectionQuery( "select f from TemporalEntity5 p join p.children c join c.friends f where p.id=1", TemporalChild5.class )
								.getResultCount();
				assertEquals( 0, friends );
			} );
		}
		pause( scope );
		var nextInstant = getInstant( scope );
		pause( scope );
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity5 entity = session.find( TemporalEntity5.class, 1L );
					entity.strings.remove( "x" );
					entity.strings.add( "z" );
					entity.children.get(0).friends.clear();
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity5 entity = session.find( TemporalEntity5.class, 1L );
					assertEquals( Set.of("y", "z"), entity.strings );
					assertEquals( 0, entity.children.get(0).friends.size() );
				}
		);
		try (var session = scope.getSessionFactory().withOptions().asOf(instant).open()) {
			scope.getSessionFactory().inTransaction(
					tx -> {
						TemporalEntity5 entity = session.find( TemporalEntity5.class, 1L );
						assertEquals( Set.of( "x" ), entity.strings );
						assertEquals( 0, entity.children.get( 0 ).friends.size() );
					}
			);
		}
		try (var session = scope.getSessionFactory().withOptions().asOf(nextInstant).open()) {
			scope.getSessionFactory().inTransaction(
					tx -> {
						TemporalEntity5 entity = session.find( TemporalEntity5.class, 1L );
						assertEquals( Set.of( "x", "y" ), entity.strings );
						assertEquals( 1, entity.children.get( 0 ).friends.size() );
					}
			);
		}
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity5 entity = session.find( TemporalEntity5.class, 1L );
					session.remove( entity.children.get(0) );
					session.remove( entity );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity5 entity = session.find( TemporalEntity5.class, 1L );
					assertNull( entity );
				}
		);
		try (var session = scope.getSessionFactory().withOptions().asOf(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity5 entity = session.find( TemporalEntity5.class, 1L );
				assertEquals( "hello", entity.text );
			} );
		}
	}

	private static void pause(SessionFactoryScope scope) throws InterruptedException {
//		scope.inSession( s -> s.doWork( connection -> connection.createStatement().execute( "ALTER SYSTEM checkpoint" ) ) );
		Thread.sleep( 4_000 );
	}

	@Test void testStateless(SessionFactoryScope scope) throws InterruptedException {
		pause( scope );
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity5 entity = new TemporalEntity5();
					entity.id = 2L;
					entity.text = "hello";
					session.insert( entity );
				}
		);
		pause( scope );
		var instant = getInstant( scope );
		pause( scope );
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity5 entity = session.get( TemporalEntity5.class, 2L );
					entity.text = "goodbye";
					session.update( entity );
				}
		);
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity5 entity = session.get( TemporalEntity5.class, 2L );
					assertEquals( "goodbye", entity.text );
					entity =
							session.createSelectionQuery( "from TemporalEntity5 where id=2", TemporalEntity5.class )
									.getSingleResult();
					assertEquals( "goodbye", entity.text );
				}
		);
		try (var session = scope.getSessionFactory().withStatelessOptions().asOf(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity5 entity = session.get( TemporalEntity5.class, 2L );
				assertEquals( "hello", entity.text );
				entity =
						session.createSelectionQuery( "from TemporalEntity5 where id=2", TemporalEntity5.class )
								.getSingleResult();
				assertEquals( "hello", entity.text );
			} );
		}
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity5 entity = session.get( TemporalEntity5.class, 2L );
					session.delete( entity );
				}
		);
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity5 entity = session.get( TemporalEntity5.class, 2L );
					assertNull( entity );
				}
		);
		try (var session = scope.getSessionFactory().withStatelessOptions().asOf(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity5 entity = session.get( TemporalEntity5.class, 2L );
				assertEquals( "hello", entity.text );
			} );
		}
	}

	private static Instant getInstant(SessionFactoryScope scope) {
		return scope.fromSession( s -> s.createQuery( "select instant", Instant.class ).getSingleResult() );
	}

	@Temporal(rowStart = "effective_from", rowEnd = "effective_to")
	@Entity(name = "TemporalEntity5")
	static class TemporalEntity5 {
		@Id
		long id;
		@Version
		int version;
		String text;
		@OneToMany(mappedBy = "parent")
		List<TemporalChild5> children = new ArrayList<>();
		@Temporal
		@ElementCollection
		Set<String> strings = new HashSet<>();
	}

	@Temporal(rowStart = "effective_from", rowEnd = "effective_to")
	@Entity(name = "TemporalChild5")
	static class TemporalChild5 {
		@Id
		long id;
		@Version
		int version;
		String text;
		@ManyToOne @JoinColumn
		TemporalEntity5 parent;

		@Temporal @ManyToMany
		Set<TemporalChild5> friends = new HashSet<>();
	}
}
