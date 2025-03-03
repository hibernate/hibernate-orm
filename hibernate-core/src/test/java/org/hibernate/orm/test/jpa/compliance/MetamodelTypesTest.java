/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.MappedSuperclassType;
import jakarta.persistence.metamodel.Metamodel;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@Jpa(annotatedClasses = {
		MetamodelTypesTest.BaseEntity.class,
		MetamodelTypesTest.Person.class,
		MetamodelTypesTest.Order.class,
})
@Jira( "https://hibernate.atlassian.net/browse/HHH-18683" )
public class MetamodelTypesTest {
	@Test
	public void getJavaType(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			final Metamodel metaModel = entityManager.getMetamodel();
			final ManagedType<Person> mTypeOrder = metaModel.managedType( Person.class );
			assertNotNull( mTypeOrder );
			final Attribute<Person, ?> attrib = mTypeOrder.getDeclaredAttribute( "age" );
			assertNotNull( attrib );
			final Class<?> pAttribJavaType = attrib.getJavaType();
			assertEquals( "int", pAttribJavaType.getName() );
		} );
	}

	@Test
	public void entityTypeTest(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			final JpaMetamodel jpaMetamodel = entityManager.getEntityManagerFactory()
					.unwrap( SessionFactoryImplementor.class ).getJpaMetamodel();

			// Existing entity type
			final EntityDomainType<Person> personType = jpaMetamodel.entity( Person.class );
			assertThat( personType ).isNotNull().isSameAs( jpaMetamodel.findEntityType( Person.class ) )
					.extracting( EntityDomainType::getName ).isEqualTo( "Person" );
			assertThat( jpaMetamodel.entity( Person.class.getName() ) ).isSameAs(
					jpaMetamodel.findEntityType( Person.class.getName() ) ).isSameAs( personType );

			// Nonexistent entity type
			assertThat( jpaMetamodel.findEntityType( Order.class ) ).isNull();
			assertThat( jpaMetamodel.findEntityType( "AnotherEntity" ) ).isNull();
			try {
				jpaMetamodel.entity( "AnotherEntity" );
				fail( "Expected IllegalArgumentException for nonexistent entity type" );
			}
			catch (IllegalArgumentException e) {
				assertThat( e ).hasMessageContaining( "Not an entity" );
			}
			try {
				jpaMetamodel.entity( Order.class.getName() );
				fail( "Expected IllegalArgumentException for embeddable type requested as entity" );
			}
			catch (IllegalArgumentException e) {
				assertThat( e ).hasMessageContaining( "Not an entity" );
			}
		} );
	}

	@Test
	public void mappedSuperclassTypeTest(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			final JpaMetamodel jpaMetamodel = entityManager.getEntityManagerFactory()
					.unwrap( SessionFactoryImplementor.class ).getJpaMetamodel();

			// Existing mapped superclass type
			final ManagedDomainType<BaseEntity> baseEntityType = jpaMetamodel.managedType( BaseEntity.class );
			assertThat( baseEntityType ).isInstanceOf( MappedSuperclassType.class )
					.isSameAs( jpaMetamodel.findManagedType( BaseEntity.class ) )
					.extracting( ManagedDomainType::getTypeName ).isEqualTo( BaseEntity.class.getName() );
			assertThat( jpaMetamodel.managedType( BaseEntity.class.getName() ) ).isSameAs(
					jpaMetamodel.findManagedType( BaseEntity.class.getName() ) ).isSameAs( baseEntityType );

			// Nonexistent mapped superclass type
			assertThat( jpaMetamodel.findManagedType( MetamodelTypesTest.class ) ).isNull();
			assertThat( jpaMetamodel.findManagedType( "AnotherEntity" ) ).isNull();
			try {
				jpaMetamodel.managedType( "AnotherEntity" );
				fail( "Expected IllegalArgumentException for nonexistent entity type" );
			}
			catch (IllegalArgumentException e) {
				assertThat( e ).hasMessageContaining( "Not a managed type" );
			}
			try {
				jpaMetamodel.managedType( MetamodelTypesTest.class );
				fail( "Expected IllegalArgumentException for embeddable type requested as entity" );
			}
			catch (IllegalArgumentException e) {
				assertThat( e ).hasMessageContaining( "Not a managed type" );
			}
		} );
	}

	@Test
	public void embeddableTypeTest(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			final JpaMetamodel jpaMetamodel = entityManager.getEntityManagerFactory()
					.unwrap( SessionFactoryImplementor.class ).getJpaMetamodel();

			// Existing mapped superclass type
			final EmbeddableDomainType<Order> embeddableType = jpaMetamodel.embeddable( Order.class );
			assertThat( embeddableType ).isNotNull().isSameAs( jpaMetamodel.findEmbeddableType( Order.class ) )
					.extracting( ManagedDomainType::getTypeName ).isEqualTo( Order.class.getName() );
			assertThat( jpaMetamodel.embeddable( Order.class.getName() ) ).isSameAs(
					jpaMetamodel.findEmbeddableType( Order.class.getName() ) ).isSameAs( embeddableType );

			// Nonexistent mapped superclass type
			assertThat( jpaMetamodel.findEmbeddableType( BaseEntity.class ) ).isNull();
			assertThat( jpaMetamodel.findEmbeddableType( "AnotherEmbeddable" ) ).isNull();
			try {
				jpaMetamodel.embeddable( "AnotherEmbeddable" );
				fail( "Expected IllegalArgumentException for nonexistent entity type" );
			}
			catch (IllegalArgumentException e) {
				assertThat( e ).hasMessageContaining( "Not an embeddable" );
			}
			try {
				jpaMetamodel.embeddable( Person.class );
				fail( "Expected IllegalArgumentException for embeddable type requested as entity" );
			}
			catch (IllegalArgumentException e) {
				assertThat( e ).hasMessageContaining( "Not an embeddable" );
			}
		} );
	}

	@MappedSuperclass
	public static abstract class BaseEntity {
		@Id
		private int id;
	}

	@Entity(name = "Person")
	public static class Person extends BaseEntity {
		private String name;

		private int age;

		@Embedded
		private Order order;
	}

	@Embeddable
	public static class Order {
		private Integer number;
		private String item;
	}
}
