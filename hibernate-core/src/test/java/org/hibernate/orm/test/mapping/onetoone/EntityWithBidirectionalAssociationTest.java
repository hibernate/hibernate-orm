/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetoone;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				EntityWithBidirectionalAssociationTest.Parent.class,
				EntityWithBidirectionalAssociationTest.Child.class
		}
)
@ServiceRegistry
@SessionFactory
public class EntityWithBidirectionalAssociationTest {
	@Test
	public void basicTest(SessionFactoryScope scope) {
		final EntityPersister parentDescriptor = scope.getSessionFactory()
				.getMappingMetamodel()
				.findEntityDescriptor( Parent.class );

		final ModelPart childAssociation = parentDescriptor.findSubPart( "child" );

		assertThat( childAssociation, instanceOf( ToOneAttributeMapping.class ) );

		final ToOneAttributeMapping childAttributeMapping = (ToOneAttributeMapping) childAssociation;

		ForeignKeyDescriptor foreignKeyDescriptor = childAttributeMapping.getForeignKeyDescriptor();
		foreignKeyDescriptor.visitKeySelectables(
				(columnIndex, selection) -> {
					assertThat( selection.getContainingTableExpression(), is( "PARENT" ) );
					assertThat( selection.getSelectionExpression(), is( "child_id" ) );
				}
		);

		foreignKeyDescriptor.visitTargetSelectables(
				(columnIndex, selection) -> {
					assertThat( selection.getContainingTableExpression(), is( "CHILD" ) );
					assertThat( selection.getSelectionExpression(), is( "id" ) );
				}
		);

		final EntityPersister childDescriptor = scope.getSessionFactory()
				.getMappingMetamodel()
				.findEntityDescriptor( Child.class );

		final ModelPart parentAssociation = childDescriptor.findSubPart( "parent" );

		assertThat( parentAssociation, instanceOf( ToOneAttributeMapping.class ) );

		final ToOneAttributeMapping parentAttributeMapping = (ToOneAttributeMapping) parentAssociation;

		foreignKeyDescriptor = parentAttributeMapping.getForeignKeyDescriptor();
		foreignKeyDescriptor.visitKeySelectables(
				(columnIndex, selection) -> {
					assertThat( selection.getContainingTableExpression(), is( "PARENT" ) );
					assertThat( selection.getSelectionExpression(), is( "child_id" ) );
				}
		);

		foreignKeyDescriptor.visitTargetSelectables(
				(columnIndex, selection) -> {
					assertThat( selection.getContainingTableExpression(), is( "CHILD" ) );
					assertThat( selection.getSelectionExpression(), is( "id" ) );
				}
		);
	}

	@Entity(name = "Parent")
	@Table(name = "PARENT")
	public static class Parent {
		private Integer id;

		private String description;
		private Child child;

		Parent() {
		}

		public Parent(Integer id, String description) {
			this.id = id;
			this.description = description;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		@OneToOne
		public Child getChild() {
			return child;
		}

		public void setChild(Child other) {
			this.child = other;
		}

	}

	@Entity(name = "Child")
	@Table(name = "CHILD")
	public static class Child {
		private Integer id;

		private String name;
		private Parent parent;

		Child() {
		}

		Child(Integer id, Parent parent) {
			this.id = id;
			this.parent = parent;
			this.parent.setChild( this );
		}

		@Id
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

		@OneToOne(mappedBy = "child")
		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}
}
