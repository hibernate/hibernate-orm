/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.nonpkassociation;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author pholvs
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/nonpkassociation/NonPkManyToOneAssociationHbmTest.hbm.xml"
)
@SessionFactory
public class NonPkManyToOneAssociationHbmTest {

	private Parent parent;

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					parent = new Parent( 99999L );
					s.persist( parent );

					Child c = new Child( parent );
					parent.getChildren().add( c );
					c.setParent( parent );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from NonPkManyToOneAssociationHbmTest$Child" ).executeUpdate();
					session.createQuery( "delete from NonPkManyToOneAssociationHbmTest$Parent" ).executeUpdate();
				}
		);
	}


	@Test
	public void testHqlWithFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					Parent dbParent = s.find( Parent.class, this.parent.getId() );
					Set<Child> children = dbParent.getChildren();
					assertEquals( 1, children.size() );
				}
		);
	}

	public static class Parent {
		private Long id;

		private Long collectionKey;

		private Set<Child> children = new HashSet<>();

		public Parent(Long collectionKey) {
			setCollectionKey( collectionKey );
		}

		Parent() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getCollectionKey() {
			return collectionKey;
		}

		public void setCollectionKey(Long collectionKey) {
			this.collectionKey = collectionKey;
		}

		public Set<Child> getChildren() {
			return children;
		}

		public void setChildren(Set<Child> children) {
			this.children = children;
		}
	}

	public static class Child {

		private Long id;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		private String name;

		private Parent parent;

		public Child(Parent parent) {
			setParent( parent );
		}

		Child() {
		}

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
