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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				BatchAndBagCollectionTest.EntityA.class,
				BatchAndBagCollectionTest.EntityB.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "10")
		}
)
@SessionFactory
@JiraKey("HHH-16570")
public class BatchAndBagCollectionTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA entityA = new EntityA( 1 );
					EntityA childA1 = new EntityA( 2 );
					EntityA childA2 = new EntityA( 3 );

					EntityB entityB1 = new EntityB();
					EntityB entityB2 = new EntityB();
					EntityB entityB3 = new EntityB();

					entityA.addChild( childA1 );
					entityA.addChild( childA2 );

					childA1.setListOfEntitiesB( List.of( entityB1, entityB2, entityB3 ) );

					session.persist( entityA );
					session.persist( childA1 );
					session.persist( childA2 );
					session.persist( entityB1 );
					session.persist( entityB2 );
					session.persist( entityB3 );
				}
		);

	}

	@Test
	public void testOneToManyHasCorrectSize(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<EntityA> entitiesA = session.createQuery(
									"select a from EntityA a where a.parent is null",
									EntityA.class
							)
							.getResultList();
					assertThat( entitiesA ).hasSize( 1 );
					EntityA entityA = entitiesA.get( 0 );
					assertThat( entityA.getId() ).isEqualTo( 1 );
					assertThat( entityA.getChildren() ).hasSize( 2 );
				}
		);
	}

	@Entity(name = "EntityA")
	@Table(name = "ENTITY_A")
	public static class EntityA {
		@Id
		Integer id;

		String name;

		@ManyToOne
		EntityA parent;

		@OneToMany(mappedBy = "parent")
		List<EntityA> children = new ArrayList<>();

		@OneToMany
		@Fetch(FetchMode.JOIN)
		List<EntityB> listOfEntitiesB = new ArrayList<>();

		public EntityA() {
		}

		public EntityA(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public List<EntityA> getChildren() {
			return children;
		}

		public void setChildren(List<EntityA> children) {
			this.children = children;
		}

		public void setListOfEntitiesB(List<EntityB> listOfEntitiesB) {
			this.listOfEntitiesB = listOfEntitiesB;
		}

		public void addChild(EntityA childA) {
			children.add( childA );
			childA.parent = this;
		}
	}

	@Entity(name = "EntityB")
	@Table(name = "ENTITY_B")
	public static class EntityB {
		@Id
		@GeneratedValue
		@Column(name = "ID")
		Integer id;

		String name;
	}
}
