/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.nonpkassociation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author pholvs
 */
public class NonPkManyToOneAssociationHbmTest extends BaseCoreFunctionalTestCase {

	public String[] getMappings() {
		return new String[] { "nonpkassociation/NonPkManyToOneAssociationHbmTest.hbm.xml" };
	}

	private Parent parent;

	@Before
	public void createTestData() {
		inTransaction(
				s -> {
					parent = new Parent( 99999L );
					s.persist( parent );

					Child c = new Child( parent );
					parent.getChildren().add( c );
					c.setParent( parent );
				}
		);
	}


	@Test
	public void testHqlWithFetch() {
		inTransaction(
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

