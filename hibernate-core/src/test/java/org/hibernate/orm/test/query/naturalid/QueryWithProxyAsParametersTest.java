/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.naturalid;

import org.hibernate.annotations.NaturalId;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				QueryWithProxyAsParametersTest.Parent.class,
				QueryWithProxyAsParametersTest.Child.class
		}
)
@SessionFactory
@JiraKey( "HHH-17881" )
public class QueryWithProxyAsParametersTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					Child child = new Child(1l, "abc", "Andrea");
					Parent parent = new Parent(2l, "Lionello", child);
					session.persist( child );
					session.persist( parent );
				}
		);
	}

	@Test
	public void testQuery(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					Child child = session.getReference( Child.class, 1l );

					Parent parent = session.createQuery( "select p from Parent p where p.child = :child", Parent.class )
							.setParameter( "child", child ).uniqueResult();

					assertThat(parent).isNotNull();
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		private Long id;

		private String name;

		@OneToOne()
		@JoinColumn(name = "child_code", referencedColumnName = "child_code")
		private Child child;

		public Parent() {
		}

		public Parent(Long id, String name, Child child) {
			this.id = id;
			this.name = name;
			this.child = child;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Child getChild() {
			return child;
		}
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		private Long id;

		@NaturalId
		@Column(name = "child_code")
		private String code;

		private String name;

		public Child() {
		}

		public Child(Long id, String code, String name) {
			this.id = id;
			this.code = code;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getCode() {
			return code;
		}

		public String getName() {
			return name;
		}
	}
}
