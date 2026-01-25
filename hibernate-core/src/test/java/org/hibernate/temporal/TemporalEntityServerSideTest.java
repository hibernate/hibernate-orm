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
		{TemporalEntityServerSideTest.TemporalEntity.class,
		TemporalEntityServerSideTest.TemporalChild.class})
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_TABLE_STRATEGY, value = "SERVER_TIMESTAMP"))
class TemporalEntityServerSideTest {

	@Test void test(SessionFactoryScope scope) throws InterruptedException {
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity entity = new TemporalEntity();
					entity.id = 1L;
					entity.text = "hello";
					entity.strings.add( "x" );
					session.persist( entity );
					TemporalChild child = new TemporalChild();
					child.id = 1L;
					child.text = "world";
					child.parent = entity;
					session.persist( child );
				}
		);
		Thread.sleep( 250 );
		var instant = Instant.now();
		Thread.sleep( 250 );
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity entity = session.find( TemporalEntity.class, 1L );
					entity.text = "goodbye";
					entity.strings.add( "y" );
					entity.children.get(0).text = "world!";
					TemporalChild friend = new TemporalChild();
					friend.id = 5L;
					friend.text = "friend";
					session.persist( friend );
					entity.children.get(0).friends.add( friend );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity entity = session.find( TemporalEntity.class, 1L );
					assertEquals( "goodbye", entity.text );
					assertEquals( 1, entity.children.size() );
					assertEquals( "world!", entity.children.get(0).text );
					assertEquals( 2, entity.strings.size() );
					assertEquals( 1, entity.children.get(0).friends.size() );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity entity =
							session.createSelectionQuery( "from TemporalEntity where id=1", TemporalEntity.class )
									.getSingleResult();
					assertEquals( "goodbye", entity.text );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity entity =
							session.createSelectionQuery( "from TemporalEntity p left join fetch p.children c where p.id=1", TemporalEntity.class )
									.getSingleResult();
					assertTrue( Hibernate.isInitialized(entity.children) );
					assertEquals( "goodbye", entity.text );
					assertEquals( 1, entity.children.size() );
					assertEquals( "world!", entity.children.get(0).text );
					var friends =
							session.createSelectionQuery( "select f from TemporalEntity p join p.children c join c.friends f where p.id=1", TemporalChild.class )
									.getResultCount();
					assertEquals( 1, friends );
				}
		);
		try (var session = scope.getSessionFactory().withOptions().instant(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity entity = session.find( TemporalEntity.class, 1L );
				assertEquals( "hello", entity.text );
				assertEquals( 1, entity.children.size() );
				assertEquals( "world", entity.children.get(0).text );
				assertEquals( 1, entity.strings.size() );
				assertEquals( 0, entity.children.get(0).friends.size() );
			} );
		}
		try (var session = scope.getSessionFactory().withOptions().instant(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity entity =
						session.createSelectionQuery( "from TemporalEntity where id=1", TemporalEntity.class )
								.getSingleResult();
				assertEquals( "hello", entity.text );
			} );
		}
		try (var session = scope.getSessionFactory().withOptions().instant(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity entity =
						session.createSelectionQuery( "from TemporalEntity p left join fetch p.children c where p.id=1", TemporalEntity.class )
								.getSingleResult();
				assertTrue( Hibernate.isInitialized(entity.children) );
				assertEquals( "hello", entity.text );
				assertEquals( 1, entity.children.size() );
				assertEquals( "world", entity.children.get(0).text );
				var friends =
						session.createSelectionQuery( "select f from TemporalEntity p join p.children c join c.friends f where p.id=1", TemporalChild.class )
								.getResultCount();
				assertEquals( 0, friends );
			} );
		}
		Thread.sleep( 250 );
		var nextInstant = Instant.now();
		Thread.sleep( 250 );
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity entity = session.find( TemporalEntity.class, 1L );
					entity.strings.remove( "x" );
					entity.strings.add( "z" );
					entity.children.get(0).friends.clear();
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity entity = session.find( TemporalEntity.class, 1L );
					assertEquals( Set.of("y", "z"), entity.strings );
					assertEquals( 0, entity.children.get(0).friends.size() );
				}
		);
		try (var session = scope.getSessionFactory().withOptions().instant(instant).open()) {
			scope.getSessionFactory().inTransaction(
					tx -> {
						TemporalEntity entity = session.find( TemporalEntity.class, 1L );
						assertEquals( Set.of( "x" ), entity.strings );
						assertEquals( 0, entity.children.get( 0 ).friends.size() );
					}
			);
		}
		try (var session = scope.getSessionFactory().withOptions().instant(nextInstant).open()) {
			scope.getSessionFactory().inTransaction(
					tx -> {
						TemporalEntity entity = session.find( TemporalEntity.class, 1L );
						assertEquals( Set.of( "x", "y" ), entity.strings );
						assertEquals( 1, entity.children.get( 0 ).friends.size() );
					}
			);
		}
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity entity = session.find( TemporalEntity.class, 1L );
					session.remove( entity );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity entity = session.find( TemporalEntity.class, 1L );
					assertNull( entity );
				}
		);
		try (var session = scope.getSessionFactory().withOptions().instant(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity entity = session.find( TemporalEntity.class, 1L );
				assertEquals( "hello", entity.text );
			} );
		}
	}

	@Test void testStateless(SessionFactoryScope scope) throws InterruptedException {
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity entity = new TemporalEntity();
					entity.id = 2L;
					entity.text = "hello";
					session.insert( entity );
				}
		);
		Thread.sleep( 250 );
		var instant = Instant.now();
		Thread.sleep( 250 );
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity entity = session.get( TemporalEntity.class, 2L );
					entity.text = "goodbye";
					session.update( entity );
				}
		);
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity entity = session.get( TemporalEntity.class, 2L );
					assertEquals( "goodbye", entity.text );
					entity =
							session.createSelectionQuery( "from TemporalEntity where id=2", TemporalEntity.class )
									.getSingleResult();
					assertEquals( "goodbye", entity.text );
				}
		);
		try (var session = scope.getSessionFactory().withStatelessOptions().instant(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity entity = session.get( TemporalEntity.class, 2L );
				assertEquals( "hello", entity.text );
				entity =
						session.createSelectionQuery( "from TemporalEntity where id=2", TemporalEntity.class )
								.getSingleResult();
				assertEquals( "hello", entity.text );
			} );
		}
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity entity = session.get( TemporalEntity.class, 2L );
					session.delete( entity );
				}
		);
		scope.getSessionFactory().inStatelessTransaction(
				session -> {
					TemporalEntity entity = session.get( TemporalEntity.class, 2L );
					assertNull( entity );
				}
		);
		try (var session = scope.getSessionFactory().withStatelessOptions().instant(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity entity = session.get( TemporalEntity.class, 2L );
				assertEquals( "hello", entity.text );
			} );
		}
	}


	@Temporal(starting = "effective_from", ending = "effective_to")
	@Entity(name = "TemporalEntity")
	static class TemporalEntity {
		@Id
		long id;
		@Version
		int version;
		String text;
		@OneToMany(mappedBy = "parent")
		List<TemporalChild> children = new ArrayList<>();
		@Temporal
		@ElementCollection
		Set<String> strings = new HashSet<>();
	}

	@Temporal(starting = "effective_from", ending = "effective_to")
	@Entity(name = "TemporalChild")
	static class TemporalChild {
		@Id
		long id;
		@Version
		int version;
		String text;
		@ManyToOne @JoinColumn
		TemporalEntity parent;

		@Temporal @ManyToMany
		Set<TemporalChild> friends = new HashSet<>();
	}
}
