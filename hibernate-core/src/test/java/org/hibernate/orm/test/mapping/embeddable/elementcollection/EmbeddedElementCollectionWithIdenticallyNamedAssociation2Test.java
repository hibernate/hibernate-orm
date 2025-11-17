/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.elementcollection;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				EmbeddedElementCollectionWithIdenticallyNamedAssociation2Test.EntityA.class,
				EmbeddedElementCollectionWithIdenticallyNamedAssociation2Test.EntityB.class,
		}
)
@SessionFactory(
		statementInspectorClass = SQLStatementInspector.class
)
public class EmbeddedElementCollectionWithIdenticallyNamedAssociation2Test {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA entityA1 = new EntityA( 1, "Fab" );
					EntityA entityA2 = new EntityA( 2, "And" );
					EntityB entityB = new EntityB( 1, "Chris" );

					ElementCollectionHolder elementCollectionHolder = new ElementCollectionHolder();
					EmbeddableB embeddableB = new EmbeddableB( entityA2 );
					elementCollectionHolder.addElementCollection( embeddableB );
					entityB.setElementCollectionHolder( elementCollectionHolder );
					entityB.setNested( new IdenticallyNamedAssociationHolder( entityA1 ) );
					entityA1.setB( entityB );

					session.persist( entityA1 );
					session.persist( entityA2 );
					session.persist( entityB );
				}
		);
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					EntityA entityA = session.get( EntityA.class, 1 );
					assertThat( entityA ).isNotNull();

					EntityB entityB = entityA.getB();
					assertThat( entityB.getNested().getIdenticallyNamedAssociation() ).isEqualTo( entityA );

					Set<EmbeddableB> elementCollection = entityB.getElementCollectionHolder().getElementCollection();
					assertThat( elementCollection.size() ).isEqualTo( 1 );

					EmbeddableB embeddableB = elementCollection.iterator().next();
					EntityA identicallyNamedAssociation = embeddableB.getNested().getIdenticallyNamedAssociation();

					assertThat( identicallyNamedAssociation ).isNotEqualTo( entityA );
					assertThat( identicallyNamedAssociation.getId()).isEqualTo( 2 );

					assertThat( identicallyNamedAssociation.getB() ).isNull();

					assertThat( statementInspector.getSqlQueries().size() ).isEqualTo( 2 );
					assertThat( statementInspector.getNumberOfJoins( 0 ) ).isEqualTo( 3 );
					assertThat( statementInspector.getNumberOfJoins( 1 ) ).isEqualTo( 4 );
				}
		);
	}

	@Entity(name = "EntityA")
	public static class EntityA {
		@Id
		int id;

		String name;

		@OneToOne(mappedBy = "nested.identicallyNamedAssociation", fetch = FetchType.EAGER)
		EntityB b;

		public EntityA() {
		}

		public EntityA(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setId(int id) {
			this.id = id;
		}

		public void setName(String name) {
			this.name = name;
		}

		public EntityB getB() {
			return b;
		}

		public void setB(EntityB b) {
			this.b = b;
		}
	}

	@Entity(name = "EntityB")
	public static class EntityB {
		@Id
		int id;

		String name;

		@Embedded
		IdenticallyNamedAssociationHolder nested;

		@Embedded
		ElementCollectionHolder elementCollectionHolder;

		public EntityB() {
		}

		public EntityB(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public ElementCollectionHolder getElementCollectionHolder() {
			return elementCollectionHolder;
		}

		public void setElementCollectionHolder(ElementCollectionHolder elementCollectionHolder) {
			this.elementCollectionHolder = elementCollectionHolder;
		}

		public void setId(int id) {
			this.id = id;
		}

		public void setName(String name) {
			this.name = name;
		}

		public IdenticallyNamedAssociationHolder getNested() {
			return nested;
		}

		public void setNested(IdenticallyNamedAssociationHolder nested) {
			this.nested = nested;
		}
	}

	@Embeddable
	public static class IdenticallyNamedAssociationHolder {

		@OneToOne
		@JoinColumn(name = "entityA_id")
		EntityA identicallyNamedAssociation;

		public IdenticallyNamedAssociationHolder() {
		}

		public IdenticallyNamedAssociationHolder(EntityA identicallyNamedAssociation) {
			this.identicallyNamedAssociation = identicallyNamedAssociation;
		}

		public EntityA getIdenticallyNamedAssociation() {
			return identicallyNamedAssociation;
		}
	}

	@Embeddable
	public static class ElementCollectionHolder {

		@ElementCollection(fetch = FetchType.EAGER)
		Set<EmbeddableB> elementCollection = new HashSet<>();

		public ElementCollectionHolder() {
		}

		public Set<EmbeddableB> getElementCollection() {
			return elementCollection;
		}

		public void setElementCollection(Set<EmbeddableB> elementCollection) {
			this.elementCollection = elementCollection;
		}

		public void addElementCollection(EmbeddableB embeddableB) {
			this.elementCollection.add( embeddableB );
		}
	}

	@Embeddable
	public static class EmbeddableB {

		@Embedded
		NestedEmbeddableB nested;

		public EmbeddableB() {
		}

		public EmbeddableB(EntityA identicallyNamedAssociation) {
			this.nested = new NestedEmbeddableB( identicallyNamedAssociation );
		}

		public NestedEmbeddableB getNested() {
			return nested;
		}
	}

	@Embeddable
	public static class NestedEmbeddableB {
		@OneToOne
		@JoinColumn(name = "entityA_id")
		EntityA identicallyNamedAssociation;

		public NestedEmbeddableB() {
		}

		public NestedEmbeddableB(EntityA identicallyNamedAssociation) {
			this.identicallyNamedAssociation = identicallyNamedAssociation;
		}

		public EntityA getIdenticallyNamedAssociation() {
			return identicallyNamedAssociation;
		}
	}
}
