/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.annotations.LazyGroup;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@DomainModel(
		annotatedClasses = {
				LazyOneToOneRemoveFlushAccessTest.ContainingEntity.class,
				LazyOneToOneRemoveFlushAccessTest.ContainedEntity.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class LazyOneToOneRemoveFlushAccessTest {

	@Test
	void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			ContainingEntity entity1 = new ContainingEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );

			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 3 );

			session.persist( containingEntity1 );
			session.persist( entity1 );
			session.persist( containedEntity );

			containingEntity1.setContained( containedEntity );
			containedEntity.setContaining( containingEntity1 );
		} );

		scope.inTransaction( session -> {
			ContainingEntity containing = session.get( ContainingEntity.class, 2 );
			ContainedEntity containedEntity = containing.getContained();

			ContainingEntity containingAsIndexedEmbedded = containedEntity.getContaining();

			session.remove( containedEntity );
			session.flush();

			ContainingEntity parent = containingAsIndexedEmbedded.getParent();

			ContainingEntity child = parent.getChild();

			assertThat( child.getId() ).isEqualTo( 2 );
		} );
	}

	@Entity(name = "containing")
	public static class ContainingEntity {

		@Id
		private Integer id;

		@OneToOne(fetch = FetchType.LAZY)
		private ContainingEntity parent;

		@OneToOne(mappedBy = "parent", fetch = FetchType.LAZY)
		private ContainingEntity child;

		@OneToOne(mappedBy = "containing")
		private ContainedEntity contained;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public ContainingEntity getParent() {
			return parent;
		}

		public void setParent(ContainingEntity parent) {
			this.parent = parent;
		}

		public ContainingEntity getChild() {
			return child;
		}

		public void setChild(ContainingEntity child) {
			this.child = child;
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

		@OneToOne(fetch = FetchType.LAZY)
		@LazyGroup("containing")
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
