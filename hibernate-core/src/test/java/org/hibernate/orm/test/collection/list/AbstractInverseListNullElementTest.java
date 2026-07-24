/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.list;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
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

	@Test
	public void testNullElementLeavesIndexGap(SessionFactoryScope scope) {
		final Long parentId = scope.fromTransaction( session -> {
			final ListParent parent = new ListParent();
			final ListChild first = new ListChild( parent );
			final ListChild last = new ListChild( parent );

			parent.children.add( first );
			parent.children.add( null );
			parent.children.add( last );

			session.persist( parent );
			session.persist( first );
			session.persist( last );

			return parent.id;
		} );

		scope.inTransaction( session -> {
			final List<Integer> positions = session.createNativeQuery(
					"select pos from ListChild where pos is not null order by pos",
					Integer.class
			).getResultList();
			assertThat( positions ).containsExactly( 0, 2 );
		} );

		scope.inTransaction( session -> {
			final ListParent parent = session.find( ListParent.class, parentId );
			assertThat( parent.children ).hasSize( 3 );
			assertThat( parent.children.get( 1 ) ).isNull();
		} );
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
