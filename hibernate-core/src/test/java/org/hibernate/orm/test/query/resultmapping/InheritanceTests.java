/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.resultmapping;

import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@DomainModel( annotatedClasses = {
		InheritanceTests.Parent.class,
		InheritanceTests.Child.class
} )
@SessionFactory
@JiraKey("HHH-17102")
public class InheritanceTests {

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new Child( 1L, "test", 123 ) );
				}
		);
	}

	@AfterEach
	public void cleanUpData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testResultSetMappingForParentEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final List<Parent> children = session.createNamedQuery( "Parent", Parent.class ).getResultList();
					assertEquals( 1, children.size() );
					assertEquals( 1L, children.get( 0 ).id );
					assertEquals( "test", children.get( 0 ).name );
					assertInstanceOf( Child.class, children.get( 0 ) );
					assertEquals( 123, ( (Child) children.get( 0 ) ).test );
				}
		);
	}

	@Test
	public void testResultSetMappingForChildEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final List<Child> children = session.createNamedQuery( "Child", Child.class ).getResultList();
					assertEquals( 1, children.size() );
					assertEquals( 1L, children.get( 0 ).id );
					assertEquals( "test", children.get( 0 ).name );
					assertEquals( 123, children.get( 0 ).test );
				}
		);
	}

	@SqlResultSetMapping(
			name = "ParentResult",
			entities = {
					@EntityResult(
							entityClass = Parent.class,
							discriminatorColumn = "discr",
							fields = {
									@FieldResult(name = "id", column = "id"),
									@FieldResult(name = "name", column = "name"),
									@FieldResult(name = "test", column = "test")
							}
					)
			}
	)
	@NamedNativeQuery(
			name = "Parent",
			query = "SELECT p.id as id, case when c.id is not null then 1 else 0 end as discr, p.name as name, c.test as test FROM parent p left join child c on p.id = c.id",
			resultSetMapping = "ParentResult"
	)
	@Entity(name = "Parent")
	@Table(name = "parent")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Parent {
		@Id
		@Column(name = "id")
		Long id;
		String name;

		public Parent() {
		}

		public Parent(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@SqlResultSetMapping(
			name = "ChildResult",
			entities = {
					@EntityResult(
							entityClass = Child.class,
							fields = {
									@FieldResult(name = "id", column = "id"),
									@FieldResult(name = "name", column = "name"),
									@FieldResult(name = "test", column = "test")
							}
					)
			}
	)
	@NamedNativeQuery(
			name = "Child",
			query = "SELECT c.id as id, 'test' as name, c.test FROM child c",
			resultSetMapping = "ChildResult"
	)
	@Entity(name = "Child")
	@Table(name = "child")
	public static class Child extends Parent {
		@Column(name = "test")
		int test;

		public Child() {
		}

		public Child(Long id, String name, int test) {
			super( id, name );
			this.test = test;
		}
	}
}
