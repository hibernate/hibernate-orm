/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-11579")
@Jpa(
		annotatedClasses = {QueryParametersWithDisabledValidationTest.TestEntity.class}
)
public class QueryParametersWithDisabledValidationTest {

	@Test
	public void setParameterWithWrongTypeShouldNotThrowIllegalArgumentException(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> entityManager.createQuery( "select e from TestEntity e where e.id = :id" ).setParameter( "id", 1 )
		);
	}

	@Test
	public void setParameterWithCorrectTypeShouldNotThrowIllegalArgumentException(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> entityManager.createQuery( "select e from TestEntity e where e.id = :id" ).setParameter( "id", 1L )
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;
	}
}
