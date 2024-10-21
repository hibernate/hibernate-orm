/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.embeddedid;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.OneToOne;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				OneToOneJoinColumnsEmbeddedIdTest.EntityA.class,
				OneToOneJoinColumnsEmbeddedIdTest.EntityB.class,
		}
)
@SessionFactory
@BytecodeEnhanced(runNotEnhancedAsWell = true)
@JiraKey("HHH-17838")
public class OneToOneJoinColumnsEmbeddedIdTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityAKey entityAKey = new EntityAKey( 1, "1" );
					EntityA entityA = new EntityA( entityAKey, "te1" );

					EntityBKey entityBKey = new EntityBKey( 1, "1" );
					EntityB entityB = new EntityB( entityBKey, entityA );

					session.persist( entityA );
					session.persist( entityB );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityAKey entityAKey = new EntityAKey( 1, "1" );
					EntityA entityA = session.find( EntityA.class, entityAKey );
					assertThat( entityA ).isNotNull();

					EntityB entityB = entityA.getEntityB();
					assertThat( entityB ).isNotNull();

					EntityBKey key = entityB.getEntityBKey();
					assertThat( key.id1 ).isEqualTo( 1 );
					assertThat( key.id2 ).isEqualTo( "1" );
				}
		);
	}

	@Test
	public void testFind2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityBKey entityBKey = new EntityBKey( 1, "1" );
					EntityB entityB = session.find( EntityB.class, entityBKey );
					assertThat( entityB ).isNotNull();

					EntityA entityA = entityB.getEntityA();
					assertThat( entityA ).isNotNull();

					EntityAKey entityAKey = entityA.getEntityAKey();
					assertThat( entityAKey.id1 ).isEqualTo( 1 );
					assertThat( entityAKey.id2 ).isEqualTo( "1" );

					assertThat( entityA.getName() ).isEqualTo( "te1" );
				}
		);
	}

	@Entity(name = "EntityA")
	public static class EntityA {

		@EmbeddedId
		private EntityAKey entityAKey;

		private String name;

		@OneToOne(mappedBy = "entityA", fetch = FetchType.LAZY)
		private EntityB entityB;

		public EntityA() {
		}

		public EntityA(EntityAKey key, String name) {
			this.entityAKey = key;
			this.name = name;
		}

		public EntityAKey getEntityAKey() {
			return entityAKey;
		}

		public String getName() {
			return name;
		}

		public EntityB getEntityB() {
			return entityB;
		}
	}

	@Embeddable
	public static class EntityAKey {

		@Column(name = "id1")
		private Integer id1;

		@Column(name = "id2")
		private String id2;

		public EntityAKey() {
		}

		public EntityAKey(Integer id1, String id2) {
			this.id1 = id1;
			this.id2 = id2;
		}
	}

	@Entity(name = "EntityB")
	public static class EntityB {
		@EmbeddedId
		private EntityBKey entityBKey;

		@OneToOne(optional = false, fetch = FetchType.LAZY)
		@JoinColumns({
				@JoinColumn(name = "id1", referencedColumnName = "id1", nullable = false,
						insertable = false, updatable = false),
				@JoinColumn(name = "id2", referencedColumnName = "id2", nullable = false,
						insertable = false, updatable = false)
		})
		private EntityA entityA;

		public EntityB() {
		}

		public EntityB(EntityBKey key, EntityA testEntity) {
			this.entityBKey = key;
			this.entityA = testEntity;
			testEntity.entityB = this;
		}

		public EntityBKey getEntityBKey() {
			return entityBKey;
		}

		public EntityA getEntityA() {
			return entityA;
		}
	}

	@Embeddable
	public static class EntityBKey {

		@Column(name = "id1")
		private Integer id1;

		@Column(name = "id2")
		private String id2;

		public EntityBKey() {
		}

		public EntityBKey(Integer documentType, String no) {
			this.id1 = documentType;
			this.id2 = no;
		}
	}
}
