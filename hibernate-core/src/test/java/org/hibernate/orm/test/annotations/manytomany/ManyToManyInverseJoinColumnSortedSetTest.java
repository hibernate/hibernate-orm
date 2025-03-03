/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytomany;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hibernate.annotations.SortNatural;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

@JiraKey(value = "HHH-16031")
public class ManyToManyInverseJoinColumnSortedSetTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testDefault() {
		inTransaction( session -> {
			ContainingEntity containing = new ContainingEntity();
			containing.setId( 0 );

			ContainedEntity contained1 = new ContainedEntity();
			contained1.setId( 1 );
			containing.getContained().add( contained1 );
			contained1.getContaining().add( containing );

			ContainedEntity contained2 = new ContainedEntity();
			contained2.setId( 2 );
			containing.getContained().add( contained2 );
			contained2.getContaining().add( containing );

			session.persist( contained1 );
			session.persist( contained2 );
			session.persist( containing );
		} );

		inTransaction( session -> {
			ContainingEntity containing = session.get( ContainingEntity.class, 0 );
			assertThat( containing.getContained() )
					.extracting( ContainedEntity::getId )
					.containsExactly( 1, 2 );
		} );

		inTransaction( session -> {
			ContainingEntity containing = session.get( ContainingEntity.class, 0 );
			ContainedEntity contained1 = session.get( ContainedEntity.class, 1 );
			contained1.getContaining().remove( containing );
			containing.getContained().remove( contained1 );
			assertThat( containing.getContained() )
					.extracting( ContainedEntity::getId )
					.containsExactly( 2 );
		} );

		// Try again from a new transaction;
		// with the bug unsolved, getContained() returns an empty collection!
		inTransaction( session -> {
			ContainingEntity containing = session.get( ContainingEntity.class, 0 );
			assertThat( containing.getContained() )
					.extracting( ContainedEntity::getId )
					.containsExactly( 2 );
		} );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				ContainingEntity.class,
				ContainedEntity.class
		};
	}

	@Entity(name = "containing")
	public static class ContainingEntity {
		@Id
		private Integer id;

		@ManyToMany
		@JoinTable(inverseJoinColumns = @JoinColumn(name = "inverse"))
		@SortNatural
		private SortedSet<ContainedEntity> contained = new TreeSet<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public SortedSet<ContainedEntity> getContained() {
			return contained;
		}

		public void setContained(SortedSet<ContainedEntity> contained) {
			this.contained = contained;
		}

	}


	@Entity(name = "contained")
	public static class ContainedEntity implements Comparable<ContainedEntity> {

		@Id
		private Integer id;

		@ManyToMany(mappedBy = "contained")
		private List<ContainingEntity> containing = new ArrayList<>();

		@Override
		public int compareTo(ContainedEntity o) {
			return getId() - o.getId();
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<ContainingEntity> getContaining() {
			return containing;
		}

	}

}
