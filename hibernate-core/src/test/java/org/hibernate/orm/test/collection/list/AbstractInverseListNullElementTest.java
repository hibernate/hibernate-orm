/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.list;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A null element of an inverse indexed one-to-many collection leaves a gap in the order column, and the gap is
 * padded back into a null element when the collection is loaded ({@code ListInitializer#readCollectionRow}).
 * <p>
 * Both {@code ActionQueue} implementations write the order column from the unowned side (see HHH-5732 and
 * HHH-18830), so both are exercised here.
 *
 * @author Donghwan Kim
 */
@JiraKey("HHH-20658")
public abstract class AbstractInverseListNullElementTest {

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testNullElementLeavesIndexGap(SessionFactoryScope scope) {
		final Long parentId = persistParentWithChildren( scope, true );

		scope.inTransaction( session -> assertThat( positions( session ) ).containsExactly( 0, 2 ) );

		assertLoadsAs( scope, parentId, 3, 1 );
	}

	@Test
	public void testQueuedAdditionAfterAPreExistingNullElement(SessionFactoryScope scope) {
		final Long parentId = persistParentWithChildren( scope, true );

		scope.inTransaction( session -> {
			final ListParent parent = session.find( ListParent.class, parentId );
			appendChild( session, parent );
		} );

		scope.inTransaction( session -> assertThat( positions( session ) ).containsExactly( 0, 2, 3 ) );

		assertLoadsAs( scope, parentId, 4, 1 );
	}

	@Test
	public void testQueuedAdditionOfANullElement(SessionFactoryScope scope) {
		final Long parentId = persistParentWithChildren( scope, false );

		scope.inTransaction( session -> {
			final ListParent parent = session.find( ListParent.class, parentId );
			parent.children.add( null );
			appendChild( session, parent );
		} );

		scope.inTransaction( session -> assertThat( positions( session ) ).containsExactly( 0, 1, 3 ) );

		assertLoadsAs( scope, parentId, 4, 2 );
	}

	@Test
	public void testUpdateAfterAPreExistingNullElement(SessionFactoryScope scope) {
		final Long parentId = persistParentWithChildren( scope, true );

		scope.inTransaction( session -> {
			final ListParent parent = session.find( ListParent.class, parentId );
			// read the collection first, so the addition goes through a collection update
			// rather than through the queued operations of an uninitialized collection
			assertThat( parent.children ).hasSize( 3 );
			appendChild( session, parent );
		} );

		scope.inTransaction( session -> assertThat( positions( session ) ).containsExactly( 0, 2, 3 ) );

		assertLoadsAs( scope, parentId, 4, 1 );
	}

	@Test
	public void testUpdateAddingANullElement(SessionFactoryScope scope) {
		final Long parentId = persistParentWithChildren( scope, false );

		scope.inTransaction( session -> {
			final ListParent parent = session.find( ListParent.class, parentId );
			assertThat( parent.children ).hasSize( 2 );
			parent.children.add( null );
			appendChild( session, parent );
		} );

		scope.inTransaction( session -> assertThat( positions( session ) ).containsExactly( 0, 1, 3 ) );

		assertLoadsAs( scope, parentId, 4, 2 );
	}

	/**
	 * Persists a parent holding two children, optionally separated by a null element.
	 */
	private static Long persistParentWithChildren(SessionFactoryScope scope, boolean withNullElement) {
		return scope.fromTransaction( session -> {
			final ListParent parent = new ListParent();
			final ListChild first = new ListChild( parent );
			final ListChild last = new ListChild( parent );

			parent.children.add( first );
			if ( withNullElement ) {
				parent.children.add( null );
			}
			parent.children.add( last );

			session.persist( parent );
			session.persist( first );
			session.persist( last );

			return parent.id;
		} );
	}

	private static void appendChild(SessionImplementor session, ListParent parent) {
		final ListChild appended = new ListChild( parent );
		parent.children.add( appended );
		session.persist( appended );
	}

	private static void assertLoadsAs(SessionFactoryScope scope, Long parentId, int size, int nullIndex) {
		scope.inTransaction( session -> {
			final ListParent parent = session.find( ListParent.class, parentId );
			assertThat( parent.children ).hasSize( size );
			assertThat( parent.children.get( nullIndex ) ).isNull();
		} );
	}

	private static List<Integer> positions(SessionImplementor session) {
		return session.createNativeQuery(
				"select pos from ListChild where pos is not null order by pos",
				Integer.class
		).getResultList();
	}

	@Entity(name = "ListParent")
	static class ListParent {
		@Id
		@GeneratedValue
		Long id;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
		@OrderColumn(name = "pos")
		List<ListChild> children = new ArrayList<>();
	}

	@Entity(name = "ListChild")
	static class ListChild {
		@Id
		@GeneratedValue
		Long id;

		@ManyToOne
		ListParent parent;

		ListChild() {
		}

		ListChild(ListParent parent) {
			this.parent = parent;
		}
	}
}
