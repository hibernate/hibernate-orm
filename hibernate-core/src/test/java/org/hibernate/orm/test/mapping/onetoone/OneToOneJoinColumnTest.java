/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.onetoone;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@TestForIssue(jiraKey = "HHH-15786")
public class OneToOneJoinColumnTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { ContainingEntity.class, ContainedEntity.class };
	}

	@Before
	public void prepare() {
		inTransaction( s -> {
			ContainingEntity entity = new ContainingEntity();
			entity.setId( 1 );
			s.persist( entity );

			assertThat( entity ).isNotNull();

			ContainedEntity contained = new ContainedEntity();
			contained.setId( 2 );

			entity.setContained( contained );
			contained.setContaining( entity );

			s.persist( contained );
		} );
	}

	@After
	public void tearDown() {
		inTransaction( s -> {
			s.createMutationQuery( "delete contained" ).executeUpdate();
			s.createMutationQuery( "delete containing" ).executeUpdate();
		} );
	}

	@Test
	public void testUpdate() {
		inTransaction( s -> {
			ContainingEntity entity = s.get( ContainingEntity.class, 1 );

			ContainedEntity contained = new ContainedEntity();
			contained.setId( 3 );

			entity.getContained().setContaining( null );
			entity.setContained( contained );
			contained.setContaining( entity );

			s.persist( contained );
		} );

		inTransaction( s -> {
			ContainingEntity entity = s.get( ContainingEntity.class, 1 );
			assertThat( entity ).isNotNull();
			assertThat( entity.getContained().getId() ).isEqualTo( 3 );
		} );
	}

	@Entity(name = "containing")
	public static class ContainingEntity {
		@Id
		private Integer id;

		@OneToOne(mappedBy = "containing")
		private ContainedEntity contained;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public ContainedEntity getContained() {
			return contained;
		}

		public void setContained(ContainedEntity contained) {
			this.contained = contained;
		}
	}

	@Entity(name = "contained")
	public static class ContainedEntity {
		@Id
		private Integer id;

		@OneToOne
		@JoinColumn(name = "containing")
		private ContainingEntity containing;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public ContainingEntity getContaining() {
			return containing;
		}

		public void setContaining(ContainingEntity containing) {
			this.containing = containing;
		}
	}
}
