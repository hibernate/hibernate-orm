/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.literal;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Nathan Xu
 */
@DomainModel(annotatedClasses = ConvertibleNullLiteralTest.Foo.class)
@SessionFactory
@JiraKey( value = "HHH-15246" )
class ConvertibleNullLiteralTest {

	@Test
	void testNoNullPointerExceptionThrown(SessionFactoryScope scope) {
		scope.inTransaction( s -> s.createQuery( "UPDATE Foo SET someEnum = NULL").executeUpdate() );
	}

	@Entity(name = "Foo")
	static class Foo {
		@Id
		int id;

		FooEnum someEnum;
	}

	enum FooEnum {
		A, B
	}
}
