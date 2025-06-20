/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable;

import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				EmbeddableAscDescQueryTest.EntityA.class,
				EmbeddableAscDescQueryTest.EntityB.class,
				EmbeddableAscDescQueryTest.EntityC.class,
				EmbeddableAscDescQueryTest.EntityD.class
		}
)
@SessionFactory
@JiraKey("HHH-16997")
public class EmbeddableAscDescQueryTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA entityA = new EntityA();
					entityA.setId( 1 );

					EntityD entityD = new EntityD();
					entityD.setId( 1 );

					ComponentB componentB = new ComponentB();
					componentB.setEntityD( entityD );

					entityA.setComponentB( componentB );

					EntityA entityA2 = new EntityA();
					entityA2.setId( 2 );

					ComponentA componentA = new ComponentA();
					componentA.setEntityA( entityA );
					entityA2.setComponentA( componentA );

					EntityB entityB = new EntityB();
					entityB.setEntityA( entityA );

					session.persist( entityA );
					session.persist( entityD );
					session.persist( entityB );
					session.persist( entityA2 );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testQueryAscAndDesc(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					session.getTransaction().begin();
					try {
						CriteriaBuilder cb = session.getCriteriaBuilder();
						CriteriaQuery<EntityA> query = cb.createQuery( EntityA.class );
						Root<EntityA> root = query.from( EntityA.class );
						query.select( root ).orderBy(
								cb.asc( root.get( "id" ) )
						);
						List<EntityA> entityAS = session.createQuery( query ).getResultList();
						assertThat( entityAS ).hasSize( 2 );

						EntityA entityA1 = entityAS.get( 0 );
						assertThat( entityA1.getId() ).isEqualTo( 1 );
						assertThat( entityA1.getComponentB() ).isNotNull();
						assertThat( entityA1.getComponentB().getEntityD() ).isNotNull();

						session.getTransaction().rollback();
						session.getTransaction().begin();

						cb = session.getCriteriaBuilder();
						query = cb.createQuery( EntityA.class );
						root = query.from( EntityA.class );
						query.select( root ).orderBy(
								cb.desc( root.get( "id" ) )
						);
						entityAS = session.createQuery( query ).getResultList();
						assertThat( entityAS ).hasSize( 2 );

						entityA1 = entityAS.get( 1 );
						assertThat( entityA1.getId() ).isEqualTo( 1 );
						assertThat( entityA1.getComponentB() ).isNotNull();
						assertThat( entityA1.getComponentB().getEntityD() ).isNotNull();
						assertThat( entityA1.getComponentB().getEntityC() ).isNull();

					}
					finally {
						session.getTransaction().rollback();
					}
				}
		);
	}

	@Test
	public void testQueryAsc(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					CriteriaBuilder cb = session.getCriteriaBuilder();
					CriteriaQuery<EntityA> query = cb.createQuery( EntityA.class );
					Root<EntityA> root = query.from( EntityA.class );
					query.select( root ).orderBy(
							cb.asc( root.get( "id" ) )
					);
					List<EntityA> entityAS = session.createQuery( query ).getResultList();
					assertThat( entityAS ).hasSize( 2 );

					EntityA entityA1 = entityAS.get( 0 );
					assertThat( entityA1.getId() ).isEqualTo( 1 );
					assertThat( entityA1.getComponentB() ).isNotNull();
					assertThat( entityA1.getComponentB().getEntityD() ).isNotNull();
					assertThat( entityA1.getComponentB().getEntityC() ).isNull();

				}
		);
	}

	@Test
	public void testQueryDesc(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					CriteriaBuilder cb = session.getCriteriaBuilder();
					CriteriaQuery<EntityA> query = cb.createQuery( EntityA.class );
					Root<EntityA> root = query.from( EntityA.class );
					query.select( root ).orderBy(
							cb.desc( root.get( "id" ) )
					);
					List<EntityA> entityAS = session.createQuery( query ).getResultList();
					assertThat( entityAS ).hasSize( 2 );

					EntityA entityA1 = entityAS.get( 1 );
					assertThat( entityA1.getId() ).isEqualTo( 1 );
					assertThat( entityA1.getComponentB() ).isNotNull();
					assertThat( entityA1.getComponentB().getEntityD() ).isNotNull();
					assertThat( entityA1.getComponentB().getEntityC() ).isNull();
				}
		);
	}


	@Test
	public void testQueryEntityB(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<EntityB> entityBS = session.createQuery( "select b from EntityB b" ).getResultList();
					assertThat( entityBS ).hasSize( 1 );

					EntityB entrityB = entityBS.get( 0 );
					EntityA entityA = entrityB.getEntityA();
					assertThat( entityA ).isNotNull();
					assertThat( entityA.getId() ).isEqualTo( 1 );
					assertThat( entityA.getComponentB() ).isNotNull();
					assertThat( entityA.getComponentB().getEntityD() ).isNotNull();
					assertThat( entityA.getComponentB().getEntityC() ).isNull();
				}
		);
	}

	@Entity(name = "EntityA")
	@Table(name = "t_entity_a")
	public static class EntityA {

		@Id
		private Integer id;

		private String name;

		@Embedded
		private ComponentA componentA;

		@Embedded
		private ComponentB componentB;

		@OneToOne(mappedBy = "entityA")
		private EntityB entityB;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EntityB getEntityB() {
			return entityB;
		}

		public void setEntityB(EntityB entityB) {
			this.entityB = entityB;
		}

		public ComponentA getComponentA() {
			return componentA;
		}

		public void setComponentA(ComponentA componentA) {
			this.componentA = componentA;
		}

		public ComponentB getComponentB() {
			return componentB;
		}

		public void setComponentB(ComponentB componentB) {
			this.componentB = componentB;
		}

	}

	@Embeddable
	public static class ComponentA {

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "cmpa_a_id", referencedColumnName = "id")
		private EntityA entityA;


		public EntityA getEntityA() {
			return entityA;
		}

		public void setEntityA(EntityA entityA) {
			this.entityA = entityA;
		}
	}

	@Embeddable
	public static class ComponentB {

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "cmpb_c_id", referencedColumnName = "id")
		private EntityC entityC;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "cmpb_d_id", referencedColumnName = "id")
		private EntityD entityD;


		public EntityC getEntityC() {
			return entityC;
		}

		public void setEntityC(EntityC entityC) {
			this.entityC = entityC;
		}

		public EntityD getEntityD() {
			return entityD;
		}

		public void setEntityD(EntityD entityD) {
			this.entityD = entityD;
		}
	}

	@Entity(name = "EntityB")
	@Table(name = "t_entity_b")
	public static class EntityB {

		@Id
		@OneToOne
		@JoinColumn(name = "id", referencedColumnName = "id")
		private EntityA entityA;

		private String name;

		public EntityA getEntityA() {
			return entityA;
		}

		public void setEntityA(EntityA entityA) {
			this.entityA = entityA;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "EntityC")
	@Table(name = "t_entity_c")
	public static class EntityC {

		@Id
		private Integer id;

		private String name;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "EntityD")
	@Table(name = "t_entity_d")
	public static class EntityD {

		@Id
		private Integer id;

		private String name;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
