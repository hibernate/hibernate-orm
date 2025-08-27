/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.manytoone.jointable;

import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				ManyToOneJoinTableTest.SimpleEntity.class,
				ManyToOneJoinTableTest.OtherEntity.class,
				ManyToOneJoinTableTest.AnotherEntity.class
		}
)
@ServiceRegistry
@SessionFactory
public class ManyToOneJoinTableTest {

	@Test
	public void basicTest(SessionFactoryScope scope) {
		final EntityPersister otherDescriptor = scope.getSessionFactory()
				.getMappingMetamodel()
				.findEntityDescriptor( OtherEntity.class );

		final ModelPart simpleEntityAssociation = otherDescriptor.findSubPart( "simpleEntity" );

		assertThat( simpleEntityAssociation, instanceOf( ToOneAttributeMapping.class ) );

		final ToOneAttributeMapping simpleAttributeMapping = (ToOneAttributeMapping) simpleEntityAssociation;

		ForeignKeyDescriptor foreignKeyDescriptor = simpleAttributeMapping.getForeignKeyDescriptor();
		foreignKeyDescriptor.visitKeySelectables(
				(columnIndex, selection) -> {
					assertThat( selection.getContainingTableExpression(), is( "other_simple" ) );
					assertThat( selection.getSelectionExpression(), is( "RHS_ID" ) );
				}
		);

		foreignKeyDescriptor.visitTargetSelectables(
				(columnIndex, selection) -> {
					assertThat( selection.getContainingTableExpression(), is( "simple_entity" ) );
					assertThat( selection.getSelectionExpression(), is( "id" ) );
				}
		);

		final ModelPart anotherEntityAssociation = otherDescriptor.findSubPart( "anotherEntity" );

		assertThat( anotherEntityAssociation, instanceOf( ToOneAttributeMapping.class ) );

		final ToOneAttributeMapping anotherAttributeMapping = (ToOneAttributeMapping) anotherEntityAssociation;

		foreignKeyDescriptor = anotherAttributeMapping.getForeignKeyDescriptor();
		foreignKeyDescriptor.visitKeySelectables(
				(columnIndex, selection) -> {
					assertThat( selection.getContainingTableExpression(), is( "other_another" ) );
					assertThat( selection.getSelectionExpression(), is( "RHS_ID" ) );
				}
		);

		foreignKeyDescriptor.visitTargetSelectables(
				(columnIndex, selection) -> {
					assertThat( selection.getContainingTableExpression(), is( "another_entity" ) );
					assertThat( selection.getSelectionExpression(), is( "id" ) );
				}
		);


		final EntityPersister simpleDescriptor = scope.getSessionFactory()
				.getMappingMetamodel()
				.findEntityDescriptor( SimpleEntity.class );

		ModelPart otherEntityEntityAssociation = simpleDescriptor.findSubPart( "other" );

		assertThat( otherEntityEntityAssociation, instanceOf( ToOneAttributeMapping.class ) );

		ToOneAttributeMapping otherAttributeMapping = (ToOneAttributeMapping) otherEntityEntityAssociation;

		foreignKeyDescriptor = otherAttributeMapping.getForeignKeyDescriptor();
		foreignKeyDescriptor.visitKeySelectables(
				(columnIndex, selection) -> {
					assertThat( selection.getContainingTableExpression(), is( "other_simple" ) );
					assertThat( selection.getSelectionExpression(), is( "LHS_ID" ) );
				}
		);

		foreignKeyDescriptor.visitTargetSelectables(
				(columnIndex, selection) -> {
					assertThat( selection.getContainingTableExpression(), is( "other_entity" ) );
					assertThat( selection.getSelectionExpression(), is( "id" ) );
				}
		);


		final EntityPersister anotherDescriptor = scope.getSessionFactory()
				.getMappingMetamodel()
				.findEntityDescriptor( AnotherEntity.class );

		otherEntityEntityAssociation = anotherDescriptor.findSubPart( "other" );

		assertThat( otherEntityEntityAssociation, instanceOf( ToOneAttributeMapping.class ) );

		otherAttributeMapping = (ToOneAttributeMapping) otherEntityEntityAssociation;

		foreignKeyDescriptor = otherAttributeMapping.getForeignKeyDescriptor();
		foreignKeyDescriptor.visitKeySelectables(
				(columnIndex, selection) -> {
					assertThat( selection.getContainingTableExpression(), is( "another_entity" ) );
					assertThat( selection.getSelectionExpression(), is( "other_id" ) );
				}
		);

		foreignKeyDescriptor.visitTargetSelectables(
				(columnIndex, selection) -> {
					assertThat( selection.getContainingTableExpression(), is( "other_entity" ) );
					assertThat( selection.getSelectionExpression(), is( "id" ) );
				}
		);

	}


	@Entity(name = "OtherEntity")
	@Table(name = "other_entity")
	public static class OtherEntity {
		private Integer id;
		private String name;

		private SimpleEntity simpleEntity;

		private AnotherEntity anotherEntity;

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

		@ManyToOne
		@JoinTable(name = "other_simple",
				joinColumns = {
						@JoinColumn(name = "LHS_ID")
				},
				inverseJoinColumns = {
						@JoinColumn(name = "RHS_ID")
				})
		public SimpleEntity getSimpleEntity() {
			return simpleEntity;
		}

		public void setSimpleEntity(SimpleEntity simpleEntity) {
			this.simpleEntity = simpleEntity;
		}

		@ManyToOne
		@JoinTable(name = "other_another",
				joinColumns = {
						@JoinColumn(name = "LHS_ID")
				},
				inverseJoinColumns = {
						@JoinColumn(name = "RHS_ID")
				})
		public AnotherEntity getAnotherEntity() {
			return anotherEntity;
		}

		public void setAnotherEntity(AnotherEntity anotherEntity) {
			this.anotherEntity = anotherEntity;
		}
	}

	@Entity(name = "SimpleEntity")
	@Table(name = "simple_entity")
	public static class SimpleEntity {
		private Integer id;
		private String name;

		private OtherEntity other;

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

		@OneToOne(mappedBy = "simpleEntity")
		public OtherEntity getOther() {
			return other;
		}

		public void setOther(OtherEntity other) {
			this.other = other;
		}
	}

	@Entity(name = "AnotherEntity")
	@Table(name = "another_entity")
	public static class AnotherEntity {
		private Integer id;
		private String name;

		private OtherEntity other;

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

		@ManyToOne
		public OtherEntity getOther() {
			return other;
		}

		public void setOther(OtherEntity other) {
			this.other = other;
		}
	}
}
