/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.embeddedid;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				OneToOneEmbeddedIdTest.EntityA.class,
				OneToOneEmbeddedIdTest.EntityB.class,
		}
)
@SessionFactory
@BytecodeEnhanced(runNotEnhancedAsWell = true)
@JiraKey("HHH-17838")
public class OneToOneEmbeddedIdTest {

	private static final String ENTITY_A_NAME = "a";
	private static final String ENTITY_B_NAME = "B";

	private static final Integer ENTITY_A_ID1 = 1;
	private static final String ENTITY_A_ID2 = "1";

	private static final Integer ENTITY_B_ID1 = 2;
	private static final String ENTITY_B_ID2 = "2";

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityAKey entityAKey = new EntityAKey( ENTITY_A_ID1, ENTITY_A_ID2 );
					EntityA entityA = new EntityA( entityAKey, ENTITY_A_NAME );

					EntityBKey entityBKey = new EntityBKey( ENTITY_B_ID1, ENTITY_B_ID2 );
					EntityB entityB = new EntityB( entityBKey, entityA, ENTITY_B_NAME );

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
					EntityAKey entityAKey = new EntityAKey( ENTITY_A_ID1, ENTITY_A_ID2 );
					EntityA entityA = session.find( EntityA.class, entityAKey );
					assertThat( entityA ).isNotNull();
					assertThat( entityA.getName() ).isEqualTo( ENTITY_A_NAME );

					EntityB entityB = entityA.getEntityB();
					assertThat( entityB ).isNotNull();

					EntityBKey key = entityB.getEntityBKey();
					assertThat( key.id1 ).isEqualTo( ENTITY_B_ID1 );
					assertThat( key.id2 ).isEqualTo( ENTITY_B_ID2 );

					assertThat( entityB.getName() ).isEqualTo( ENTITY_B_NAME );
				}
		);
	}

	@Test
	public void testFind2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityBKey entityBKey = new EntityBKey( ENTITY_B_ID1, ENTITY_B_ID2 );
					EntityB entityB = session.find( EntityB.class, entityBKey );
					assertThat( entityB ).isNotNull();
					assertThat( entityB.getName() ).isEqualTo( ENTITY_B_NAME );

					EntityA entityA = entityB.getEntityA();
					assertThat( entityA ).isNotNull();

					EntityAKey entityAKey = entityA.getEntityAKey();
					assertThat( entityAKey.id1 ).isEqualTo( ENTITY_A_ID1 );
					assertThat( entityAKey.id2 ).isEqualTo( ENTITY_A_ID2 );

					assertThat( entityA.getName() ).isEqualTo( ENTITY_A_NAME );
				}
		);
	}

	@Test
	public void testNativeQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String query = "select a.* from ENTITY_A a";
					List<EntityA> entityAS = session.createNativeQuery( query, EntityA.class ).list();
					assertThat( entityAS.size() ).isEqualTo( 1 );
					EntityA entityA = entityAS.get( 0 );
					assertThat( entityA.getName() ).isEqualTo( ENTITY_A_NAME );
					assertThat( entityA.getEntityB().getName() ).isEqualTo( ENTITY_B_NAME );
				}
		);
	}

	@Test
	public void testNativeQuery2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String query = "select b.* from ENTITY_B b";
					List<EntityB> entityBS = session.createNativeQuery( query, EntityB.class ).list();
					assertThat( entityBS.size() ).isEqualTo( 1 );

					EntityB entityB = entityBS.get( 0 );
					assertThat( entityB.getName()).isEqualTo( ENTITY_B_NAME );
					assertThat( entityB.getEntityA().getName()).isEqualTo( ENTITY_A_NAME );
				}
		);
	}

	@Entity(name = "EntityA")
	@Table(name= "ENTITY_A")
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
	@Table(name = "ENTITY_B")
	public static class EntityB {
		@EmbeddedId
		private EntityBKey entityBKey;

		private String name;

		@OneToOne(optional = false, fetch = FetchType.LAZY)
//		@JoinColumns({
//				@JoinColumn(name = "id1", referencedColumnName = "id1", nullable = false,
//						insertable = false, updatable = false),
//				@JoinColumn(name = "id2", referencedColumnName = "id2", nullable = false,
//						insertable = false, updatable = false)
//		})
		private EntityA entityA;

		public EntityB() {
		}

		public EntityB(EntityBKey key, EntityA testEntity, String name) {
			this.entityBKey = key;
			this.entityA = testEntity;
			testEntity.entityB = this;
			this.name = name;
		}

		public EntityBKey getEntityBKey() {
			return entityBKey;
		}

		public EntityA getEntityA() {
			return entityA;
		}

		public String getName() {
			return name;
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
