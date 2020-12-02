/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.callbacks;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestForIssue( jiraKey = "HHH-13020" )
@DomainModel(annotatedClasses = {
		ProtectedConstructorTest.Parent.class,
		ProtectedConstructorTest.Child.class
})
@SessionFactory
public class ProtectedConstructorTest {

	@Test
	public void test(SessionFactoryScope scope) {
		Child child = new Child();

		scope.inTransaction(
				session -> session.persist( child )
		);

		scope.inTransaction(
				session -> {
					Child childReference = session.getReference( Child.class, child.getId() );
					assertEquals( child.getParent().getName(), childReference.getParent().getName() );
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {

		private Integer id;
		private String name;

		protected Parent() {
			name = "Empty";
		}

		public Parent(String s) {
			this.name = s;
		}

		@Id
		@Column(name = "id")
		@GeneratedValue(strategy = GenerationType.AUTO)
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}


	@Entity(name = "Child")
	public static class Child {

		private Integer id;
		private Parent parent;

		public Child() {
			this.parent = new Parent( "Name" );
		}

		@Id
		@Column(name = "id")
		@GeneratedValue(strategy = GenerationType.AUTO)
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}
}
