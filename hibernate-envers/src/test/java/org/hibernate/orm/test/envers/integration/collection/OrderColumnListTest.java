/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-7940")
public class OrderColumnListTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Parent.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1 - Create indexed entries.
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			Parent p = new Parent( 1 );
			p.getChildren().add( "child1" );
			p.getChildren().add( "child2" );
			entityManager.persist( p );
		} );

		// Revision 2 - remove an indexed entry, resetting positions.
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final Parent p = entityManager.find( Parent.class, 1 );
			// should remove child with id 1
			p.getChildren().remove( 0 );
			entityManager.merge( p );
		} );

		// Revision 3 - add new indexed entity to reset positions
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final Parent p = entityManager.find( Parent.class, 1 );
			// add child with id 3
			p.getChildren().add( 0, "child3" );
			entityManager.merge( p );
		} );

		// Revision 4 - remove all children
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final Parent p = entityManager.find( Parent.class, 1 );
			p.getChildren().clear();
			entityManager.merge( p );
		} );
	}

	@Test
	public void testRevisionCounts() {
		assertEquals( Arrays.asList( 1, 2, 3, 4 ), getAuditReader().getRevisions( Parent.class, 1 ) );
	}

	@Test
	public void testIndexedCollectionRev1() {
		final Parent p = getAuditReader().find( Parent.class, 1, 1 );
		assertEquals( 2, p.getChildren().size() );
		assertEquals( Arrays.asList( "child1", "child2" ), p.getChildren() );
	}

	@Test
	public void testIndexedCollectionRev2() {
		final Parent p = getAuditReader().find( Parent.class, 1, 2 );
		assertEquals( 1, p.getChildren().size() );
		assertEquals( Arrays.asList( "child2" ), p.getChildren() );
	}

	@Test
	public void testIndexedCollectionRev3() {
		final Parent p = getAuditReader().find( Parent.class, 1, 3 );
		assertEquals( 2, p.getChildren().size() );
		assertEquals( Arrays.asList( "child3", "child2" ), p.getChildren() );
	}

	@Test
	public void testIndexedCollectionRev4() {
		final Parent p = getAuditReader().find( Parent.class, 1, 4 );
		assertEquals( 0, p.getChildren().size() );
	}

	@Audited
	@Entity(name = "Parent")
	public static class Parent {
		@Id
		private Integer id;

		@ElementCollection
		@OrderColumn
		private List<String> children = new ArrayList<String>();

		Parent() {

		}

		Parent(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<String> getChildren() {
			return children;
		}

		public void setChildren(List<String> children) {
			this.children = children;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Parent parent = (Parent) o;

			return id != null ? id.equals( parent.id ) : parent.id == null;

		}

		@Override
		public int hashCode() {
			return id != null ? id.hashCode() : 0;
		}

		@Override
		public String toString() {
			return "Parent{" +
					"id=" + id +
					", children=" + children +
					'}';
		}
	}
}
