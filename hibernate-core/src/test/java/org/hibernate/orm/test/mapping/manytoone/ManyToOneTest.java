/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.manytoone;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
				ManyToOneTest.SimpleEntity.class,
				ManyToOneTest.OtherEntity.class
		}
)
@ServiceRegistry
@SessionFactory
public class ManyToOneTest {

	@Test
	public void basicTest(SessionFactoryScope scope) {
		final EntityPersister otherDescriptor = scope.getSessionFactory()
				.getMappingMetamodel()
				.findEntityDescriptor( OtherEntity.class );

		final ModelPart simpleEntityAssociation = otherDescriptor.findSubPart( "simpleEntity" );

		assertThat( simpleEntityAssociation, instanceOf( ToOneAttributeMapping.class ) );

		final ToOneAttributeMapping childAttributeMapping = (ToOneAttributeMapping) simpleEntityAssociation;

		ForeignKeyDescriptor foreignKeyDescriptor = childAttributeMapping.getForeignKeyDescriptor();
		foreignKeyDescriptor.visitKeySelectables(
				(columnIndex, selection) -> {
					assertThat( selection.getContainingTableExpression(), is( "other_entity" ) );
					assertThat( selection.getSelectionExpression(), is( "simple_entity_id" ) );
				}
		);

		foreignKeyDescriptor.visitTargetSelectables(
				(columnIndex, selection) -> {
					assertThat( selection.getContainingTableExpression(), is( "simple_entity" ) );
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
		@JoinColumn(name = "simple_entity_id")
		public SimpleEntity getSimpleEntity() {
			return simpleEntity;
		}

		public void setSimpleEntity(SimpleEntity simpleEntity) {
			this.simpleEntity = simpleEntity;
		}

	}

	@Entity(name = "SimpleEntity")
	@Table(name = "simple_entity")
	public static class SimpleEntity {
		private Integer id;
		private String name;

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
	}

}
