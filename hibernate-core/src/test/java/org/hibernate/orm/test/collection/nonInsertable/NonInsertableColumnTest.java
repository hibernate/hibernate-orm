/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.nonInsertable;

import java.util.Arrays;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@JiraKey(value = "HHH-13236")
@DomainModel(
		annotatedClasses = {
				NonInsertableColumnTest.Parent.class,
				NonInsertableColumnTest.Child.class
		}
)
@SessionFactory
public class NonInsertableColumnTest {

	@Test
	public void test(SessionFactoryScope scope) {
		Long parentId = scope.fromTransaction(
				session -> {
					Child child = new Child();
					child.field = "Test";
					child.nonInsertable = "nonInsertable";
					child.nonUpdatable = "nonUpdatable";

					Parent parent = new Parent();
					parent.children = Arrays.asList( child );

					session.persist( parent );

					session.flush();

					return parent.id;
				}
		);

		scope.inSession(
				session -> {
					Parent loaded = session.get( Parent.class, parentId );
					assertEquals( "nonUpdatable", loaded.children.get( 0 ).nonUpdatable );
					assertNull( loaded.children.get( 0 ).nonInsertable );
					assertEquals( "Test", loaded.children.get( 0 ).field );
					assertEquals( "Test", loaded.children.get( 0 ).shadowField );
				}
		);

	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		public Long id;

		@ElementCollection
		public List<Child> children;
	}

	@Embeddable
	public static class Child {

		@Column(name = "field")
		public String field;

		@Column(insertable = false)
		public String nonInsertable;

		@Column(updatable = false)
		public String nonUpdatable;

		@Column(name = "field", insertable = false, updatable = false)
		public String shadowField;
	}

}
