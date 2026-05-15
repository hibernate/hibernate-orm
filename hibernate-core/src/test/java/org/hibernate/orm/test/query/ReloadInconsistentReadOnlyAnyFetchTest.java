/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		ReloadInconsistentReadOnlyAnyFetchTest.EntityA.class,
		ReloadInconsistentReadOnlyAnyFetchTest.EntityB.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-19273")
public class ReloadInconsistentReadOnlyAnyFetchTest {

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
			newAEntity.bType = "B";
			session.persist( newAEntity );

			session.createQuery( "from EntityA a", EntityA.class ).getResultList();

			assertThat( newAEntity.b ).isNull();

			final var persister = session.getFactory().getMappingMetamodel().getEntityDescriptor( EntityB.class );
			final var entityKey = session.generateEntityKey( 1L, persister );
			final var reference = session.getPersistenceContext().getEntity( entityKey );
			assertThat( reference ).isNull();
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
		@Column(name = "b_type")
		private String bType;
		@Any(fetch = FetchType.LAZY)
		@AnyKeyJavaClass(Long.class)
		@JoinColumn(name = "b_id", insertable = false, updatable = false) //the foreign key column
		@Column(name = "b_type", insertable = false, updatable = false)   //the discriminator column
		@AnyDiscriminatorValue(discriminator = "B", entity = EntityB.class)
		private Object b;
	}

	@Entity(name = "EntityB")
	public static class EntityB {
		@Id
		private Long id;
		private String data;

		public EntityB() {
		}

		public EntityB(Long id, String data) {
			this.id = id;
			this.data = data;
		}
	}
}
