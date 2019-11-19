/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.metamodel.mapping.manytoone;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.SingularAssociationAttributeMapping;
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
				.getMetamodel()
				.findEntityDescriptor( OtherEntity.class );

		final ModelPart simpleEntityAssociation = otherDescriptor.findSubPart( "simpleEntity" );

		assertThat( simpleEntityAssociation, instanceOf( SingularAssociationAttributeMapping.class ) );

		final SingularAssociationAttributeMapping childAttributeMapping = (SingularAssociationAttributeMapping) simpleEntityAssociation;

		ForeignKeyDescriptor foreignKeyDescriptor = childAttributeMapping.getForeignKeyDescriptor();
		foreignKeyDescriptor.visitReferringColumns( (keyTable, keyColumn, jdbcMapping) -> {
			assertThat( keyTable, is( "other_entity" ) );
			assertThat( keyColumn, is( "simple_entity_id" ) );
		} );

		foreignKeyDescriptor.visitTargetColumns( (targetTable, targetColumn, jdbcMapping) -> {
			assertThat( targetTable, is( "simple_entity" ) );
			assertThat( targetColumn, is( "id" ) );
		} );

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
