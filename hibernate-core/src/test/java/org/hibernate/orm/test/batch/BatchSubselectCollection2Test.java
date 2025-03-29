/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				BatchSubselectCollection2Test.EntityA.class,
				BatchSubselectCollection2Test.EntityB.class,
				BatchSubselectCollection2Test.EntityC.class,
				BatchSubselectCollection2Test.EntityD.class,
				BatchSubselectCollection2Test.EntityE.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "10"),
				@Setting(name = AvailableSettings.FORMAT_SQL, value = "false"),
		}
)
@JiraKey("HHH-16569")
public class BatchSubselectCollection2Test {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA entityA = new EntityA();
					EntityB entityB = new EntityB();
					EntityB entityB1 = new EntityB();
					EntityC entityC = new EntityC();
					EntityD entityD = new EntityD();
					EntityD entityD1 = new EntityD();
					EntityE entityE = new EntityE();

					entityA.entityB = entityB;

					entityB.entityD = entityD;
					entityB.listOfEntitiesC.add( entityC );

					entityC.entityB = entityB;

					entityD.listOfEntitiesB.add( entityB );
					entityD.openingB = entityB;

					entityD1.openingB = entityB1;

					entityE.entityD = entityD;

					session.persist( entityA );
					session.persist( entityB );
					session.persist( entityB1 );
					session.persist( entityC );
					session.persist( entityD );
					session.persist( entityD1 );
					session.persist( entityE );
				}
		);
	}

	@Test
	public void testSelectEntityE(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<EntityE> entitiesE = session.createQuery( "select e from EntityE e", EntityE.class )
							.getResultList();
					assertThat( entitiesE ).hasSize( 1 );
					EntityE entityE = entitiesE.get( 0 );
					EntityD entityD = entityE.getEntityD();
					assertThat( entityD ).isNotNull();
					assertThat( entityD.getClosingB() ).isNull();
					EntityB openingB = entityD.getOpeningB();
					assertThat( openingB ).isNotNull();
					assertThat( openingB.getListOfEntitiesC() ).hasSize( 1 );
				}
		);
	}

	@Test
	public void testSelectingEntityAAfterSelectingEntityE(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<EntityE> entitiesE = session.createQuery( "select e from EntityE e", EntityE.class )
							.getResultList();
					assertThat( entitiesE ).hasSize( 1 );
					EntityE entityE = entitiesE.get( 0 );
					EntityD entityD = entityE.getEntityD();
					assertThat( entityD ).isNotNull();
					assertThat( entityD.getClosingB() ).isNull();
					EntityB openingB = entityD.getOpeningB();
					assertThat( openingB ).isNotNull();
					assertThat( openingB.getListOfEntitiesC() ).hasSize( 1 );


					List<EntityA> entitiesA = session.createQuery( "select a from EntityA a", EntityA.class )
							.getResultList();
					assertThat( entitiesA ).hasSize( 1 );
					EntityB entityB = entitiesA.get( 0 ).getEntityB();
					assertThat( entityB ).isNotNull();
					assertThat( entityB ).isSameAs( openingB );
					assertThat( openingB.getListOfEntitiesC() ).hasSize( 1 );
				}
		);
	}


	@Test
	public void testSelectEntityA(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<EntityA> entitiesA = session.createQuery( "select a from EntityA a", EntityA.class )
							.getResultList();
					assertThat( entitiesA ).hasSize( 1 );
					EntityB entityB = entitiesA.get( 0 ).getEntityB();
					assertThat( entityB ).isNotNull();
					assertThat( entityB.getListOfEntitiesC() ).hasSize( 1 );
				}
		);
	}

	@Test
	public void testGetEntityD(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityD entityD = session.get( EntityD.class, 1 );
					assertThat( entityD ).isNotNull();
					EntityB entityB = entityD.getOpeningB();
					assertThat( entityB ).isNotNull();

					assertThat( entityB.getListOfEntitiesC() ).hasSize( 1 );
				}
		);
	}

	@Test
	public void testSelectEntityDWithJoins(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<EntityD> entityDs = session.createQuery(
							"from EntityD d  left join fetch d.openingB left join fetch d.closingB" ).list();
					assertThat( entityDs.size() ).isEqualTo( 2 );
					EntityD entityD = entityDs.get( 0 );
					assertThat( entityD ).isNotNull();
					EntityB entityB = entityD.getOpeningB();
					assertThat( entityB ).isNotNull();
					assertThat( entityB.getListOfEntitiesC() ).hasSize( 1 );
				}
		);
	}

	@Test
	public void testSelectEntityD(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<EntityD> entitiesD = session.createQuery( "select d from EntityD d", EntityD.class )
							.getResultList();
					assertThat( entitiesD ).hasSize( 2 );
					EntityB entityB = entitiesD.get( 0 ).getOpeningB();
					assertThat( entityB ).isNotNull();
					assertThat( entityB.getListOfEntitiesC() ).hasSize( 1 );
				}
		);
	}

	@Entity(name = "EntityA")
	@Table(name = "ENTITY_A")
	public static class EntityA {
		@Id
		@GeneratedValue
		Integer id;

		@JoinColumn(name = "ENTITY_B")
		@ManyToOne
		EntityB entityB;

		public Integer getId() {
			return id;
		}

		public EntityB getEntityB() {
			return entityB;
		}
	}

	@Entity(name = "EntityB")
	@Table(name = "ENTITY_B")
	public static class EntityB {
		@Id
		@GeneratedValue
		Integer id;

		@OneToMany(mappedBy = "entityB")
		@Fetch(FetchMode.SUBSELECT)
		List<EntityC> listOfEntitiesC = new ArrayList<>();

		@JoinColumn(name = "ENTITY_D")
		@ManyToOne
		EntityD entityD;

		public Integer getId() {
			return id;
		}

		public List<EntityC> getListOfEntitiesC() {
			return listOfEntitiesC;
		}

		public EntityD getEntityD() {
			return entityD;
		}
	}

	@Entity(name = "EntityC")
	@Table(name = "ENTITY_C")
	public static class EntityC {
		@Id
		@GeneratedValue
		Integer id;

		@JoinColumn(name = "ENTITY_B")
		@ManyToOne
		EntityB entityB;

		public Integer getId() {
			return id;
		}

		public EntityB getEntityB() {
			return entityB;
		}
	}

	@Entity(name = "EntityD")
	@Table(name = "ENTITY_D")
	public static class EntityD {
		@Id
		@GeneratedValue
		Integer id;

		@JoinColumn(name = "OPENING_B")
		@ManyToOne
		EntityB openingB;

		@JoinColumn(name = "CLOSING_B")
		@ManyToOne
		EntityB closingB;

		@OneToMany(mappedBy = "entityD")
		List<EntityB> listOfEntitiesB = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public EntityB getOpeningB() {
			return openingB;
		}

		public EntityB getClosingB() {
			return closingB;
		}

		public List<EntityB> getListOfEntitiesB() {
			return listOfEntitiesB;
		}
	}

	@Entity(name = "EntityE")
	@Table(name = "ENTITY_E")
	public static class EntityE {
		@Id
		@GeneratedValue
		Integer id;

		@JoinColumn(name = "ENTITY_D")
		@ManyToOne
		EntityD entityD;

		public Integer getId() {
			return id;
		}

		public EntityD getEntityD() {
			return entityD;
		}
	}
}
