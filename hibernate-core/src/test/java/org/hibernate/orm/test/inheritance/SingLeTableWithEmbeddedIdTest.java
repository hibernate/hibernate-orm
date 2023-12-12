package org.hibernate.orm.test.inheritance;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.orm.test.hql.CompositeIdEntity;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				SingLeTableWithEmbeddedIdTest.Entity1.class,
				SingLeTableWithEmbeddedIdTest.Entity2.class,
				SingLeTableWithEmbeddedIdTest.Parent.class,
				SingLeTableWithEmbeddedIdTest.Entity3.class,
				SingLeTableWithEmbeddedIdTest.Entity4.class,
		}
)
@JiraKey("HHH-17525")
public class SingLeTableWithEmbeddedIdTest {

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Entity1 entity1 = new Entity1( 1L, "entity1" );
					entityManager.persist( entity1 );

					Entity2 entity2 = new Entity2( 2L, "entity2" );
					entityManager.persist( entity2 );

					Entity3 entity3 = new Entity3( new CompositeId( entity1, entity2 ), "entity3" );

					entityManager.persist( entity3 );

					Entity4 entity4 = new Entity4( 4L );
					entity4.addEntity( entity3 );
					entityManager.persist( entity4 );
				}
		);
	}

	@Test
	public void testHqlQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Entity4 entity4 = entityManager.createQuery( "select e from Entity4 e ", Entity4.class )
							.getSingleResult();
					Set<Entity3> entities = entity4.getEntities();
					assertThat(entities.size()).isEqualTo( 1 );
					Entity3 entity3 = entities.iterator().next();
					assertThat( entity3.getField()).isEqualTo( "entity3" );
				}
		);
	}


	@Entity(name = "Entity1")
	public static class Entity1 {

		@Id
		Long id;

		@Column
		String field;

		public Entity1() {
		}

		public Entity1(Long id, String field) {
			this.id = id;
			this.field = field;
		}

		public Long getId() {
			return id;
		}

		public String getField() {
			return field;
		}
	}

	@Entity(name = "Entity2")
	public static class Entity2 {

		@Id
		Long id;

		@Column
		String field;

		public Entity2() {
		}

		public Entity2(Long id, String field) {
			this.id = id;
			this.field = field;
		}

		public Long getId() {
			return id;
		}

		public String getField() {
			return field;
		}
	}

	public static class CompositeId implements Serializable {

		@ManyToOne
		Entity1 id1;

		@ManyToOne
		Entity2 id2;

		public CompositeId() {
		}

		public CompositeId(Entity1 id1, Entity2 id2) {
			this.id1 = id1;
			this.id2 = id2;
		}

		public Entity1 getId1() {
			return id1;
		}


		public Entity2 getId2() {
			return id2;
		}

	}


	@Entity(name = "Parent")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class Parent {
		@EmbeddedId
		CompositeId id;

		public Parent() {
		}

		public Parent(CompositeId id) {
			this.id = id;
		}

		public CompositeId getId() {
			return id;
		}

		public void setId(CompositeId id) {
			this.id = id;
		}

	}

	@Entity(name = "Entity3")
	public static class Entity3 extends Parent {

		String field;

		public Entity3() {
			super();
		}

		public Entity3(CompositeId id, String field) {
			super( id );
			this.field = field;
		}

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}
	}

	@Entity(name = "Entity4")
	public static class Entity4 {
		@Id
		public Long id;

		public String field;

		@ManyToMany
		@JoinTable(
				name = "join_table",
				joinColumns = { @JoinColumn(name = "entity3_id") },
				inverseJoinColumns = {
						@JoinColumn(name = "composite_id_entity_id1"),
						@JoinColumn(name = "composite_id_entity_id2")
				}
		)
		protected Set<Entity3> entities = new HashSet<>();

		public Entity4() {
		}

		public Entity4(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public Set<Entity3> getEntities() {
			return entities;
		}

		public void addEntity(Entity3 entity) {
			this.entities.add( entity );
		}
	}


}
