/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.spi.metadatabuildercontributor;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.NaturalId;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
public abstract class AbstractSqlFunctionMetadataBuilderContributorTest
		extends EntityManagerFactoryBasedFunctionalTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Employee.class,
		};
	}


	@Override
	protected void addConfigOptions(Map options) {
		options.put(
				EntityManagerFactoryBuilderImpl.METADATA_BUILDER_CONTRIBUTOR,
				matadataBuilderContributor()
		);
	}

	protected abstract Object matadataBuilderContributor();

	final Employee employee = new Employee();

	@Test
	public void test() {
		inTransaction( entityManager -> {
			employee.id = 1L;
			employee.username = "user@acme.com";

			entityManager.persist( employee );
		} );

		inTransaction( entityManager -> {
			int result = entityManager.createQuery(
							"select INSTR(e.username,'@acme.com') " +
							"from Employee e " +
							"where " +
							"	e.id = :employeeId", Integer.class )
					.setParameter( "employeeId", employee.id )
					.getSingleResult();

			assertThat( result ).isEqualTo( 5 );
		} );
	}

	@Entity(name = "Employee")
	public static class Employee {

		@Id
		private Long id;

		@NaturalId
		private String username;

		private String password;
	}

}
