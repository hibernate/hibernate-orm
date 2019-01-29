/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.crud.onetomany.bidirectional;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * @author Andrea Boriero
 */
public class OneToManyListInverseSideTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Parent.class, Child.class };
	}

	private Long parentId;

	@Test
	public void testOperations() {
		sessionFactoryScope().inTransaction(
				session -> {
					Child child = new Child( "Leonardo" );
					Child child2 = new Child( "Alessandro" );
					session.save( child );
					session.save( child2 );
					Parent parent = new Parent();
					parent.addChild( child );
					parent.addChild( child2 );
					session.save( parent );
					parentId = parent.getId();
				}
		);

		sessionFactoryScope().inTransaction(
				session -> {
					Parent parent = session.get( Parent.class, parentId );
					assertThat( parent.getChildren().size(), is( 2 ) );
				}
		);

		sessionFactoryScope().inTransaction(
				session -> {
					Parent parent = session.get( Parent.class, parentId );
					parent.getChildren().remove( 0 );
				}
		);

		sessionFactoryScope().inTransaction(
				session -> {
					Parent parent = session.get( Parent.class, parentId );
					assertThat( parent.getChildren().size(), is( 1 ) );
				}
		);

		sessionFactoryScope().inTransaction(
				session -> {
					Child child = new Child( "Veronica" );
					session.save( child );
					Parent parent = session.get( Parent.class, parentId );
					parent.addChild( child );
				}
		);
	}


	@Entity(name = "Parent")
	public static class Parent {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@OneToMany(mappedBy = "parent", orphanRemoval = true)
		@OrderColumn
		private List<Child> children = new ArrayList<>();

		public Parent() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void addChild(Child child) {
			children.add( child );
			child.setParent( this );
		}

		public void addChild(Child child, int i) {
			children.add( i, child );
			child.setParent( this );
		}

		public void removeChild(Child child) {
			children.remove( child );
			child.setParent( null );
		}
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@Column(nullable = false)
		private String name;

		@ManyToOne
		private Parent parent;

		public Child() {
		}

		public Child(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		@Override
		public String toString() {
			return "Child{" +
					"id=" + id +
					", name='" + name + '\'' +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Child child = (Child) o;

			return name.equals( child.name );

		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}
	}
}
