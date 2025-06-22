/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = {
				EmbeddableAndFetchModeSelectTest.EntityA.class,
				EmbeddableAndFetchModeSelectTest.EntityB.class,
				EmbeddableAndFetchModeSelectTest.EntityC.class,
				EmbeddableAndFetchModeSelectTest.EntityD.class,
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "2")
		}
)
@SessionFactory
@JiraKey("HHH-16811")
public class EmbeddableAndFetchModeSelectTest {
	private static final Integer ID_A = 1;
	private static final Integer ID_A1 = 2;
	private static final Integer ID_B = 2;

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityC entityC = new EntityC();
					EntityB entityB = new EntityB( ID_B, "B" );

					EntityA entityA = new EntityA( ID_A, new MyEmbeddableComponent( "some-ref", entityC ), entityB );
					EntityA entityA1 = new EntityA(
							ID_A1,
							new MyEmbeddableComponent( "some-ref_2", entityC ),
							entityB
					);
					EntityD entityD = new EntityD();
					entityA.setEntityD( entityD );

					session.persist( entityC );
					session.persist( entityB );
					session.persist( entityA );
					session.persist( entityA1 );
					session.persist( entityD );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA entityA = session.find( EntityA.class, ID_A );
					assertIsInitialized( entityA, false );

					EntityB entityB = session.find( EntityB.class, ID_B );
					assertThat( entityB.getListOfEntityA() ).hasSize( 2 );
				}
		);

		scope.inTransaction(
				session -> {
					EntityA entityA = session.find( EntityA.class, ID_A );

					assertIsInitialized( entityA, false );
				}
		);
	}

	@Test
	public void testFind2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA entityA1 = session.getReference( EntityA.class, ID_A1 );
					assertFalse( Hibernate.isInitialized( entityA1 ) );

					EntityA entityA = session.find( EntityA.class, ID_A );

					assertIsInitialized( entityA, false );
					assertIsInitialized( entityA1, false );

					EntityB entityB = session.find( EntityB.class, ID_B );
					assertThat( entityB.getListOfEntityA() ).hasSize( 2 );
				}
		);

		scope.inTransaction(
				session -> {
					EntityA entityA = session.find( EntityA.class, ID_A );
					assertIsInitialized( entityA, false );
				}
		);
	}

	@Test
	public void testUpdateEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA entityA = session.find( EntityA.class, ID_A );

					assertIsInitialized( entityA, false );

					entityA.setMyEmbeddable( null );
					session.find( EntityA.class, ID_A );

					EntityB entityB = session.find( EntityB.class, ID_B );
					assertThat( entityB.getListOfEntityA() ).hasSize( 2 );
				}
		);

		scope.inTransaction(
				session -> {
					EntityA entityA = session.find( EntityA.class, ID_A );

					assertIsInitialized( entityA, true );
				}
		);
	}

	@Test
	public void testUpdateEntity2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA entityA1 = session.getReference( EntityA.class, ID_A1 );
					assertFalse( Hibernate.isInitialized( entityA1 ) );

					EntityA entityA = session.find( EntityA.class, ID_A );

					assertIsInitialized( entityA, false );
					assertIsInitialized( entityA1, false );

					entityA.setMyEmbeddable( null );

					EntityB entityB = session.find( EntityB.class, ID_B );
					assertThat( entityB.getListOfEntityA() ).hasSize( 2 );
				}
		);

		scope.inTransaction(
				session -> {
					EntityA entityA = session.find( EntityA.class, ID_A );

					assertIsInitialized( entityA, true );
				}
		);
	}

	@Test
	public void testUpdateEntity3(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA entityA = session.find( EntityA.class, ID_A );

					assertIsInitialized( entityA, false );


					entityA.setEntityD( null );

					EntityB entityB = session.find( EntityB.class, ID_B );
					assertThat( entityB.getListOfEntityA() ).hasSize( 2 );
				}
		);

		scope.inTransaction(
				session -> {
					EntityA entityA = session.find( EntityA.class, ID_A );

					assertIsInitialized( entityA, false );
					assertThat( entityA.getEntityD() ).isNull();
				}
		);
	}

	@Test
	public void testUpdateEntity4(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA entityA1 = session.getReference( EntityA.class, ID_A1 );
					assertFalse( Hibernate.isInitialized( entityA1 ) );

					EntityA entityA = session.find( EntityA.class, ID_A );

					assertIsInitialized( entityA, false );
					assertIsInitialized( entityA1, false );
					assertThat( entityA.getEntityD() ).isNotNull();
					assertThat( entityA1.getEntityD() ).isNull();

					entityA.setEntityD( null );

					EntityB entityB = session.find( EntityB.class, ID_B );
					assertThat( entityB.getListOfEntityA() ).hasSize( 2 );
				}
		);

		scope.inTransaction(
				session -> {
					EntityA entityA = session.find( EntityA.class, ID_A );

					assertIsInitialized( entityA, false );
					assertThat( entityA.getEntityD() ).isNull();
				}
		);
	}

	private static void assertIsInitialized(EntityA entityA, boolean isMyEmbeddableNull) {
		MyEmbeddableComponent myEmbeddable = entityA.getMyEmbeddable();
		if ( isMyEmbeddableNull ) {
			assertThat( myEmbeddable ).isNull();
		}
		else {
			assertThat( myEmbeddable ).isNotNull();
			assertTrue( Hibernate.isInitialized( myEmbeddable.getEntityC() ) );
		}
	}

	@org.hibernate.annotations.DynamicUpdate
	@Entity(name = "EntityA")
	@Table(name = "ENTITY_A")
	public static class EntityA {
		@Id
		@Column(name = "ID")
		Integer id;

		@Embedded
		MyEmbeddableComponent myEmbeddable;

		@ManyToOne
		@JoinColumn(name = "ENTITY_D")
		@Fetch(FetchMode.SELECT)
		EntityD entityD;

		@JoinColumn(name = "ENTITY_B")
		@ManyToOne
		EntityB entityB;

		public EntityA() {
		}

		public EntityA(Integer id, MyEmbeddableComponent myEmbeddable, EntityB entityB) {
			this.id = id;
			this.myEmbeddable = myEmbeddable;
			this.entityB = entityB;
			entityB.addEntityA( this );
		}

		public Integer getId() {
			return id;
		}

		public MyEmbeddableComponent getMyEmbeddable() {
			return myEmbeddable;
		}

		public void setMyEmbeddable(MyEmbeddableComponent myEmbeddable) {
			this.myEmbeddable = myEmbeddable;
		}

		public EntityB getEntityB() {
			return entityB;
		}

		public EntityD getEntityD() {
			return entityD;
		}

		public void setEntityD(EntityD entityD) {
			this.entityD = entityD;
		}
	}

	@Entity(name = "EntityB")
	@Table(name = "ENTITY_B")
	public static class EntityB {
		@Id
		@Column(name = "ID")
		Integer id;

		String name;

		@OneToMany(mappedBy = "entityB")
		final List<EntityA> listOfEntityA = new ArrayList<>();

		public EntityB() {
		}

		public EntityB(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public List<EntityA> getListOfEntityA() {
			return listOfEntityA;
		}

		public void addEntityA(EntityA entityA) {
			listOfEntityA.add( entityA );
		}
	}

	@Entity(name = "EntityC")
	@Table(name = "ENTITY_C")
	public static class EntityC {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		@Column(name = "ID")
		Integer id;

		String name;
	}

	@Entity(name = "EntityD")
	@Table(name = "ENTITY_D")
	public static class EntityD {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		@Column(name = "ID")
		Integer id;

		String name;
	}

	@Embeddable
	public static class MyEmbeddableComponent {

		@Column(name = "MY_EMB_REF")
		String embRef;

		@ManyToOne
		@JoinColumn(name = "ENTITY_C")
		@Fetch(FetchMode.SELECT)
		EntityC entityC;

		public MyEmbeddableComponent() {
		}

		public MyEmbeddableComponent(String embRef, EntityC entityC) {
			this.embRef = embRef;
			this.entityC = entityC;
		}

		public String getEmbRef() {
			return embRef;
		}

		public EntityC getEntityC() {
			return entityC;
		}
	}
}
