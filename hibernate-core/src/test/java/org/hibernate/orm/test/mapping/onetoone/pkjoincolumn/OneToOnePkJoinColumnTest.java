/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.onetoone.pkjoincolumn;

import java.io.Serializable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;

import jakarta.persistence.PrimaryKeyJoinColumn;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@JiraKey("HHH-16463")
@Jpa(annotatedClasses = {OneToOnePkJoinColumnTest.Parent.class, OneToOnePkJoinColumnTest.Child.class},
		useCollectingStatementInspector = true)
public class OneToOnePkJoinColumnTest {

	static final String PARENT_ID = "parent_key";
	static final String CHILD_ID = "child_key";

	@Test
	public void test(EntityManagerFactoryScope scope) {
		SQLStatementInspector collectingStatementInspector = scope.getCollectingStatementInspector();

		scope.inTransaction(
				entityManager -> {
					Parent parent = new Parent();
					parent.setId(2L);
					Child child = new Child();
					parent.setChild(child);
					child.setParent(parent);
					entityManager.persist(parent);
				}
		);
		collectingStatementInspector.clear();
		scope.inTransaction(
				entityManager -> {
					Parent parent = entityManager.find( Parent.class, 2L );
					assertNotNull(parent.getChild());
				}
		);
		collectingStatementInspector
				.assertNumberOfJoins(0, 1);
		collectingStatementInspector
				.assertNumberOfOccurrenceInQueryNoSpace(0, CHILD_ID, 2);
		collectingStatementInspector
				.assertNumberOfOccurrenceInQueryNoSpace(0, PARENT_ID, 3);
	}

	@Entity(name = "Parent")
	static class Parent implements Serializable {

		@Id
		@Column(name = PARENT_ID)
		private Long id;

		//@PrimaryKeyJoinColumn
		@OneToOne(mappedBy = "parent", cascade = CascadeType.PERSIST)
		private Child child;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Child getChild() {
			return child;
		}

		public void setChild(Child optionalData) {
			this.child = optionalData;
		}

	}

	@Entity(name = "Child")
	static class Child implements Serializable {

		@Id
		private Long id;

		// this is the preferred way to do it I believe,
		// but Hibernate never recognized this mapping
		@MapsId
		@OneToOne
		@PrimaryKeyJoinColumn(name = CHILD_ID)
		private Parent parent;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

	}
}
