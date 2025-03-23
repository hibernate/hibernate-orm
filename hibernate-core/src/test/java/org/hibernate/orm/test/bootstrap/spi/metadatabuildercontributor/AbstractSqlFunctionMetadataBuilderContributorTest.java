/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.spi.metadatabuildercontributor;

import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.NaturalId;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
@JiraKey( value = "HHH-12589" )
public abstract class AbstractSqlFunctionMetadataBuilderContributorTest extends BaseEntityManagerFunctionalTestCase {

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

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			employee.id = 1L;
			employee.username = "user@acme.com";

			entityManager.persist( employee );
		} );
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			int result = entityManager.createQuery(
				"select INSTR(e.username,'@acme.com') " +
				"from Employee e " +
				"where " +
				"	e.id = :employeeId", Integer.class )
			.setParameter( "employeeId", employee.id )
			.getSingleResult();

			assertEquals( 5, result );
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
