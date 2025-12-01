/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.domain.gambit.SimpleEntity;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey( value = "HHH-11397")
@DomainModel( annotatedClasses = {SimpleEntity.class, QueryParametersValidationTest.EntityWithBasicArray.class} )
@SessionFactory( exportSchema = false )
public class QueryParametersValidationTest {
	@Test
	public void testSetParameterWithWrongType(SessionFactoryScope scope) {
		// SimpleEntity#id is of type Integer
		scope.inTransaction( (session) ->
				session.createQuery( "from SimpleEntity e where e.id = :p" )
						.setParameter( "p", 1L )
		);
	}

	@Test
	public void testSetParameterWithArrayWithNullElement(SessionFactoryScope scope) {
		// SimpleEntity#id is of type Integer
		scope.inTransaction( (session) ->
				session.createQuery( "from EntityWithBasicArray e where e.strings = :p" )
						.setParameter( "p", new String[]{null, "something"} )
		);
	}

	@Test
	public void testSetParameterWithArrayWithNullElementWrongType(SessionFactoryScope scope) {
		// SimpleEntity#id is of type Integer
		assertThrows( IllegalArgumentException.class, () ->
				scope.inTransaction( (session) ->
						session.createQuery( "from EntityWithBasicArray e where e.strings = :p" )
								.setParameter( "p", new Integer[]{null, 1} )
				)
		);
	}

	@Entity(name = "EntityWithBasicArray")
	public static class EntityWithBasicArray {
		@Id
		private Long id;
		private String name;
		private String[] strings;
	}
}
