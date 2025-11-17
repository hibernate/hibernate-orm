/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.elementcollection;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				EmbeddedElementCollectionWithIdenticallyNamedAssociationTest.EntityA.class,
				EmbeddedElementCollectionWithIdenticallyNamedAssociationTest.EntityB.class,
				EmbeddedElementCollectionWithIdenticallyNamedAssociationTest.EmbeddableB.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-15604")
public class EmbeddedElementCollectionWithIdenticallyNamedAssociationTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		EntityA a1 = new EntityA();
		a1.setId( 1 );
		EntityB b1 = new EntityB();
		b1.setId( 1 );
		b1.setIdenticallyNamedAssociation( a1 );
		a1.setB( b1 );

		EntityA a2 = new EntityA();
		a2.setId( 2 );
		EntityB b2 = new EntityB();
		b2.setId( 2 );
		b2.setIdenticallyNamedAssociation( a2 );
		a2.setB( b2 );

		EmbeddableB embeddableB = new EmbeddableB();
		embeddableB.setIdenticallyNamedAssociation( a2 );
		b1.getElementCollection().add( embeddableB );

		scope.inTransaction( session -> {
			session.persist( a1 );
			session.persist( a2 );
			session.persist( b1 );
			session.persist( b2 );
		} );

		assertThat( b1.getIdenticallyNamedAssociation() ).isEqualTo( a1 );

		assertThat( b1.getElementCollection() ).hasSize( 1 );
		assertThat( b1.getElementCollection().get( 0 ).getIdenticallyNamedAssociation() ).isEqualTo( a2 );

		assertThat( a2.getB() ).isEqualTo( b2 );
	}

	@Test
	public void testGetEntityA(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityA a1 = session.get( EntityA.class, 1 );

			EntityB b1 = a1.getB();
			assertThat( b1.getId() ).isEqualTo( 1 );
			assertThat( b1.getElementCollection() ).hasSize( 1 );
			assertThat( b1.getIdenticallyNamedAssociation()).isEqualTo( a1 );

			EntityA identicallyNamedAssociation = b1.getElementCollection().get( 0 ).getIdenticallyNamedAssociation();
			assertThat( identicallyNamedAssociation.getId() ).isEqualTo( 2 );

			EntityB b = identicallyNamedAssociation.getB();
			assertThat( b.getId() ).isEqualTo( 2 );
			assertThat( b.getElementCollection().size()).isEqualTo( 0 );
		} );
	}

	@Test
	public void testGetEntities(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityA a1 = session.get( EntityA.class, 1 );
			EntityA a2 = session.get( EntityA.class, 2 );
			EntityB b1 = session.get( EntityB.class, 1 );
			EntityB b2 = session.get( EntityB.class, 2 );
			assertThat( a1 ).isNotNull();
			assertThat( a2 ).isNotNull();
			assertThat( b1 ).isNotNull();

			assertThat( b1.getIdenticallyNamedAssociation() ).isEqualTo( a1 );

			assertThat( a1.getB() ).isEqualTo( b1 );
			assertThat( b1.getElementCollection() ).hasSize( 1 );
			EntityA identicallyNamedAssociation = b1.getElementCollection().get( 0 ).getIdenticallyNamedAssociation();
			assertThat( identicallyNamedAssociation ).isEqualTo( a2 );

			assertThat( identicallyNamedAssociation.getB() ).isEqualTo( b2 );
		} );
	}

	@Entity(name = "entityA")
	public static class EntityA {
		@Id
		private Integer id;

		private String name;

		@OneToOne(mappedBy = "identicallyNamedAssociation", fetch = FetchType.EAGER)
		private EntityB b;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EntityB getB() {
			return b;
		}

		public void setB(EntityB b) {
			this.b = b;
		}

		@Override
		public String toString() {
			return "EntityA{" +
					"id=" + id +
					'}';
		}
	}

	@Entity(name = "entityB")
	public static class EntityB {
		@Id
		private Integer id;

		private String name;

		@OneToOne
		@JoinColumn(name = "entityA_id")
		private EntityA identicallyNamedAssociation;

		@ElementCollection(fetch = FetchType.EAGER)
		@OrderColumn(name = "idx")
		@CollectionTable(name = "elemcollect")
		private List<EmbeddableB> elementCollection = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EntityA getIdenticallyNamedAssociation() {
			return identicallyNamedAssociation;
		}

		public void setIdenticallyNamedAssociation(EntityA a) {
			this.identicallyNamedAssociation = a;
		}

		public List<EmbeddableB> getElementCollection() {
			return elementCollection;
		}

		@Override
		public String toString() {
			return "EntityB{" +
					"id=" + id +
					", identicallyNamedAssociation=" + identicallyNamedAssociation +
					", elementCollection=" + elementCollection +
					'}';
		}
	}

	@Embeddable
	public static class EmbeddableB {
		@OneToOne
		@JoinColumn(name = "emb_entityA_id")
		private EntityA identicallyNamedAssociation;

		public EntityA getIdenticallyNamedAssociation() {
			return identicallyNamedAssociation;
		}

		public void setIdenticallyNamedAssociation(EntityA a) {
			this.identicallyNamedAssociation = a;
		}
	}

}
