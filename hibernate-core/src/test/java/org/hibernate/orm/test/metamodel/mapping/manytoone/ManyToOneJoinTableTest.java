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
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
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
				.getMetamodel()
				.findEntityDescriptor( OtherEntity.class );

		final ModelPart simpleEntityAssociation = otherDescriptor.findSubPart( "simpleEntity" );

		assertThat( simpleEntityAssociation, instanceOf( SingularAssociationAttributeMapping.class ) );

		final SingularAssociationAttributeMapping simpleAttributeMapping = (SingularAssociationAttributeMapping) simpleEntityAssociation;

		ForeignKeyDescriptor foreignKeyDescriptor = simpleAttributeMapping.getForeignKeyDescriptor();
		foreignKeyDescriptor.visitReferringColumns( (keyTable, keyColumn, jdbcMapping) -> {
			assertThat( keyTable, is( "other_simple" ) );
			assertThat( keyColumn, is( "RHS_ID" ) );
		} );

		foreignKeyDescriptor.visitTargetColumns( (targetTable, targetColumn, jdbcMapping) -> {
			assertThat( targetTable, is( "simple_entity" ) );
			assertThat( targetColumn, is( "id" ) );
		} );

		final ModelPart anotherEntityAssociation = otherDescriptor.findSubPart( "anotherEntity" );

		assertThat( anotherEntityAssociation, instanceOf( SingularAssociationAttributeMapping.class ) );

		final SingularAssociationAttributeMapping anotherAttributeMapping = (SingularAssociationAttributeMapping) anotherEntityAssociation;

		foreignKeyDescriptor = anotherAttributeMapping.getForeignKeyDescriptor();
		foreignKeyDescriptor.visitReferringColumns( (keyTable, keyColumn, jdbcMapping) -> {
			assertThat( keyTable, is( "other_another" ) );
			assertThat( keyColumn, is( "RHS_ID" ) );
		} );

		foreignKeyDescriptor.visitTargetColumns( (targetTable, targetColumn, jdbcMapping) -> {
			assertThat( targetTable, is( "another_entity" ) );
			assertThat( targetColumn, is( "id" ) );
		} );


		final EntityPersister simpleDescriptor = scope.getSessionFactory()
				.getMetamodel()
				.findEntityDescriptor( SimpleEntity.class );

		ModelPart otherEntityEntityAssociation = simpleDescriptor.findSubPart( "other" );

		assertThat( otherEntityEntityAssociation, instanceOf( SingularAssociationAttributeMapping.class ) );

		SingularAssociationAttributeMapping otherAttributeMapping = (SingularAssociationAttributeMapping) otherEntityEntityAssociation;

		foreignKeyDescriptor = otherAttributeMapping.getForeignKeyDescriptor();
		foreignKeyDescriptor.visitReferringColumns( (keyTable, keyColumn, jdbcMapping) -> {
			assertThat( keyTable, is( "other_simple" ) );
			assertThat( keyColumn, is( "LHS_ID" ) );
		} );

		foreignKeyDescriptor.visitTargetColumns( (targetTable, targetColumn, jdbcMapping) -> {
			assertThat( targetTable, is( "other_entity" ) );
			assertThat( targetColumn, is( "id" ) );
		} );


		final EntityPersister anotherDescriptor = scope.getSessionFactory()
				.getMetamodel()
				.findEntityDescriptor( AnotherEntity.class );

		otherEntityEntityAssociation = anotherDescriptor.findSubPart( "other" );

		assertThat( otherEntityEntityAssociation, instanceOf( SingularAssociationAttributeMapping.class ) );

		otherAttributeMapping = (SingularAssociationAttributeMapping) otherEntityEntityAssociation;

		foreignKeyDescriptor = otherAttributeMapping.getForeignKeyDescriptor();
		foreignKeyDescriptor.visitReferringColumns( (keyTable, keyColumn, jdbcMapping) -> {
			assertThat( keyTable, is( "another_entity" ) );
			assertThat( keyColumn, is( "other_id" ) );
		} );

		foreignKeyDescriptor.visitTargetColumns( (targetTable, targetColumn, jdbcMapping) -> {
			assertThat( targetTable, is( "other_entity" ) );
			assertThat( targetColumn, is( "id" ) );
		} );

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
