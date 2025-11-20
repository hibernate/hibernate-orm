/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.util.Set;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		ReloadInconsistentReadOnlyEagerToOneTest.EntityA.class,
		ReloadInconsistentReadOnlyEagerToOneTest.EntityB.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-19273")
public class ReloadInconsistentReadOnlyEagerToOneTest {

	@Test
	public void testJoinFetchInconsistentNullAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var newB = new EntityB( 1L, "abc" );
			session.persist( newB );
			session.flush();
			session.clear();

			// When
			var newAEntity = new EntityA();
			newAEntity.id = 1L;
			newAEntity.bId = 1L;
			session.persist( newAEntity );

			// EntityInitializerImpl errors when seeing `null` for A#b in the
			// entity of the persistence context, but an FK is fetched
			session.createQuery( "from EntityA a join fetch a.b", EntityA.class ).getResultList();

			assertThat( newAEntity.b ).isNull();

			final var persister = session.getFactory().getMappingMetamodel().getEntityDescriptor( EntityB.class );
			final var entityKey = session.generateEntityKey( 1L, persister );
			final var reference = session.getPersistenceContext().getEntity( entityKey );
			assertThat( reference ).isNotNull();
			assertThat( Hibernate.isInitialized( reference ) ).isTrue();
		} );
	}

	@Test
	public void testJoinFetchInconsistentNonNullAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var newB = new EntityB( 1L, "abc" );
			session.persist( newB );
			session.flush();
			session.clear();

			// When
			var newAEntity = new EntityA();
			newAEntity.id = 1L;
			newAEntity.b = newB;
			session.persist( newAEntity );

			// EntityInitializerImpl errors when seeing `null` for A#b in the
			// entity of the persistence context, but an FK is fetched
			session.createQuery( "from EntityA a join fetch a.b", EntityA.class ).getResultList();

			final var persister = session.getFactory().getMappingMetamodel().getEntityDescriptor( EntityB.class );
			final var entityKey = session.generateEntityKey( 1L, persister );
			final var reference = session.getPersistenceContext().getEntity( entityKey );
			assertThat( reference ).isNull();
		} );
	}

	@Test
	public void testSelectFetchInconsistentNullAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var newB = new EntityB( 1L, "abc" );
			session.persist( newB );
			session.flush();
			session.clear();

			// When
			var newAEntity = new EntityA();
			newAEntity.id = 1L;
			newAEntity.bId = 1L;
			session.persist( newAEntity );

			session.createQuery( "from EntityA a", EntityA.class ).getResultList();

			assertThat( newAEntity.b ).isNull();

			final var persister = session.getFactory().getMappingMetamodel().getEntityDescriptor( EntityB.class );
			final var entityKey = session.generateEntityKey( 1L, persister );
			final var reference = session.getPersistenceContext().getEntity( entityKey );
			assertThat( reference ).isNotNull();
			assertThat( Hibernate.isInitialized( reference ) ).isTrue();
		} );
	}

	@Test
	public void testSelectFetchInconsistentNonNullAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var newB = new EntityB( 1L, "abc" );
			session.persist( newB );
			session.flush();
			session.clear();

			// When
			var newAEntity = new EntityA();
			newAEntity.id = 1L;
			newAEntity.b = newB;
			session.persist( newAEntity );

			session.createQuery( "from EntityA a", EntityA.class ).getResultList();

			final var persister = session.getFactory().getMappingMetamodel().getEntityDescriptor( EntityB.class );
			final var entityKey = session.generateEntityKey( 1L, persister );
			final var reference = session.getPersistenceContext().getEntity( entityKey );
			assertThat( reference ).isNull();
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "EntityA")
	public static class EntityA {
		@Id
		private Long id;
		@Column(name = "b_id")
		private Long bId;
		@ManyToOne
		@JoinColumn(name = "b_id", insertable = false, updatable = false)
		private EntityB b;
	}

	@Entity(name = "EntityB")
	public static class EntityB {
		@Id
		private Long id;
		private String data;
		@OneToMany(mappedBy = "b")
		private Set<EntityA> as;

		public EntityB() {
		}

		public EntityB(Long id, String data) {
			this.id = id;
			this.data = data;
		}
	}
}
