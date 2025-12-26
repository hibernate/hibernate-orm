/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batchfetch;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.TypedQuery;
import org.hibernate.annotations.BatchSize;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
		annotatedClasses = {
				BatchFetchUnownedToOneTest.Parent.class,
				BatchFetchUnownedToOneTest.Child.class,
		}
)
@SessionFactory(useCollectingStatementInspector = true)
@Jira("https://hibernate.atlassian.net/browse/HHH-19235")
public class BatchFetchUnownedToOneTest {

	@BeforeEach
	void setup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					for ( int id = 0; id < 5; id++ ) {
						Parent parent = new Parent( id );
						new Child( id + 100, parent );
						session.persist( parent );
					}
				}
		);
	}

	@Test
	public void test(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.clear();
					TypedQuery<Parent> query = session.createQuery( "from Parent", Parent.class );
					List<Parent> parents = query.getResultList();
					assertEquals( 5, parents.size() );
					assertEquals( 2, statementInspector.getSqlQueries().size() );
				}
		);
	}

	@Entity(name = "Parent")
	@BatchSize(size = 20)
	public static class Parent {
		@Id
		private long id;

		@OneToOne(mappedBy = "parent", cascade = CascadeType.ALL)
		private Child child;

		Parent() {
		}

		public Parent(long id) {
			this.id = id;
		}

		public Child getChild() {
			return child;
		}

		void setChild(Child child) {
			this.child = child;
		}

		@Override
		public String toString() {
			return "Parent [id=" + id + ", child=" + child + "]";
		}
	}


	@Entity
	@BatchSize(size = 20)
	public static class Child {
		@Id
		private long id;

		@OneToOne(fetch = FetchType.LAZY)
		private Parent parent;

		Child() {
		}

		public Child(long id, Parent parent) {
			this.id = id;
			setParent( parent );
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
			parent.setChild( this );
		}

		@Override
		public String toString() {
			return "Child [id=" + id + "]";
		}
	}
}
