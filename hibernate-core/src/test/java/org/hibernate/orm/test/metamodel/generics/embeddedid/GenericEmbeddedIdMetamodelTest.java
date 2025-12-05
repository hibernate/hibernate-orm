/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.metamodel.generics.embeddedid;

import jakarta.persistence.criteria.Path;
import org.hibernate.SessionFactory;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Marco Belladelli
 */
@Jpa(annotatedClasses = {
		BaseEntity.class,
		PersonEntity.class,
		PersonId.class,
		EmployeeEntity.class,
		EmployeeId.class,
})
public class GenericEmbeddedIdMetamodelTest {
	@SuppressWarnings({"unchecked", "rawtypes"})
	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			var criteriaBuilder = entityManager.getCriteriaBuilder();
			var query = criteriaBuilder.createQuery( EmployeeEntity.class );

			var employee = query.from( EmployeeEntity.class );
			var person = query.from( PersonEntity.class );

			final Path employeeId = employee.get( EmployeeEntity_.id );
			// The Path.getModel() method returns the generic (Object) type, whereas our getResolvedModel()
			// returns the correct EmployeeId type.
			assertThat( employeeId.getModel().getBindableJavaType() ).isEqualTo( Object.class );
			assertThat( ((SqmPath<?>) employeeId).getResolvedModel().getBindableJavaType() ).isEqualTo( EmployeeId.class );
			final Path employeePersonId = employeeId.get( EmployeeId_.personId );
			assertThat( employeePersonId.getModel().getBindableJavaType() ).isEqualTo( PersonId.class );
			final var personId = person.get( PersonEntity_.id );
			// Same as before: generic vs resolved type
			assertThat( personId.getModel().getBindableJavaType() ).isEqualTo( Object.class );
			assertThat( ((SqmPath<?>) employeeId).getResolvedModel().getBindableJavaType() ).isEqualTo( EmployeeId.class );

			var equal = criteriaBuilder.equal(
					employeePersonId,
					personId
			);

			query.select( employee ).where( equal );

			final var result = entityManager.createQuery( query ).getSingleResult();
			assertThat( result.getName() ).isEqualTo( "Employee One" );
			assertThat( result.getId().getPersonId().getIdentifier() ).isEqualTo( 1L );
		} );
	}

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.persist( new PersonEntity( new PersonId( 1L ), "Person One" ) );
			entityManager.persist( new PersonEntity( new PersonId( 2L ), "Person Two" ) );
			entityManager.persist(
					new EmployeeEntity( new EmployeeId( "E001", new PersonId( 1L ) ), "Employee One", 1001 ) );
		} );
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().unwrap( SessionFactory.class ).getSchemaManager().truncateMappedObjects();
	}
}
