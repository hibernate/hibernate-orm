/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
