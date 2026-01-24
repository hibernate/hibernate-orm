/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal;

import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Temporal;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@DomainModel(annotatedClasses =
		{TemporalEntityTest.TemporalEntity.class,
		TemporalEntityTest.TemporalChild.class})
class TemporalEntityTest {

	@Test void test(SessionFactoryScope scope) throws InterruptedException {
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity entity = new TemporalEntity();
					entity.id = 1L;
					entity.text = "hello";
					session.persist( entity );
					TemporalChild child = new TemporalChild();
					child.id = 1L;
					child.text = "world";
					child.parent = entity;
					session.persist( child );
				}
		);
		var instant = Instant.now();
		Thread.sleep( 2_000 );
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity entity = session.find( TemporalEntity.class, 1L );
					entity.text = "goodbye";
					entity.children.get(0).text = "world!";
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					TemporalEntity entity = session.find( TemporalEntity.class, 1L );
					assertEquals( "goodbye", entity.text );
					assertEquals( 1, entity.children.size() );
					assertEquals( "world!", entity.children.get(0).text );
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
				}
		);
		try (var session = scope.getSessionFactory().withOptions().instant(instant).open()) {
			session.inTransaction( tx -> {
				TemporalEntity entity = session.find( TemporalEntity.class, 1L );
				assertEquals( "hello", entity.text );
				assertEquals( 1, entity.children.size() );
				assertEquals( "world", entity.children.get(0).text );
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
		var instant = Instant.now();
		Thread.sleep( 2_000 );
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
		List<TemporalChild> children;
	}

	@Temporal(starting = "effective_from", ending = "effective_to")
	@Entity(name = "TemporalChild")
	static class TemporalChild {
		@Id
		long id;
		@Version
		int version;
		String text;
		@ManyToOne @JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
		TemporalEntity parent;
	}
}
