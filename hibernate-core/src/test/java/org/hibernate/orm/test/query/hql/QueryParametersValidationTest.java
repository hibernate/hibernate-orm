/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@JiraKey( value = "HHH-11397")
@DomainModel( annotatedClasses = QueryParametersValidationTest.TestEntity.class )
@SessionFactory( exportSchema = false )
public class QueryParametersValidationTest {
	@Test
	public void setParameterWithWrongTypeShouldNotThrowIllegalArgumentException(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.createQuery( "select e from TestEntity e where e.id = :id" )
						.setParameter( "id", 1 )
		);
	}

	@Entity(name = "TestEntity")
	public class TestEntity {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;
		private String name;
	}
}
