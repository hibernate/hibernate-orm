/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.annotations;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Table;
import org.hibernate.annotations.AnyDiscriminatorImplicitValues;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Set;

@SessionFactory
@DomainModel(annotatedClasses = {ManyToAnyDefaultedJoinColumnsTest.Parent.class,
		ManyToAnyDefaultedJoinColumnsTest.Child1.class, ManyToAnyDefaultedJoinColumnsTest.Child2.class})
class ManyToAnyDefaultedJoinColumnsTest {
	@Test void test(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Parent parent = new Parent();
			Child1 child1 = new Child1();
			Child2 child2 = new Child2();
			parent.children = Set.of( child1, child2 );
			s.persist( parent );
		} );
	}
	@Entity
	@Table(name = "PARENT")
	static class Parent {
		@Id @GeneratedValue
		Long id;

		@ManyToAny(cascade = CascadeType.PERSIST)
		@JoinTable(name = "PARENT_CHILD")
		@AnyKeyJavaClass( String.class )
		@Column(name = "CHILD_TYPE")
		@AnyDiscriminatorImplicitValues
		Set<Object> children;
	}
	@Entity
	@Table(name = "CHILD_1")
	static class Child1 {
		@Id @GeneratedValue
		String id;
	}
	@Entity
	@Table(name = "CHILD_2")
	static class Child2 {
		@Id @GeneratedValue
		String id;
	}
}
