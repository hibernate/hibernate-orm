/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.lazytoone;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				LazyOneToOneWithCastTest.ContainingEntity.class,
				LazyOneToOneWithCastTest.ContainedEntity.class,
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true)
class LazyOneToOneWithCastTest {

	@Test
	void oneNullOneNotNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );

			ContainedEntity contained1 = new ContainedEntity();
			contained1.setId( 4 );
			contained1.setContainingAsIndexedEmbeddedWithCast( containingEntity1 );
			containingEntity1.setContainedIndexedEmbeddedWithCast( contained1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );

		} );

		scope.inTransaction( session -> {
			ContainedEntity contained = session.find( ContainedEntity.class, 4 );

			ContainingEntity containingAsIndexedEmbedded = contained.getContainingAsIndexedEmbedded();
			assertThat( containingAsIndexedEmbedded ).isNull();
			assertThat( Hibernate.isPropertyInitialized( contained, "containingAsIndexedEmbedded" ) ).isTrue();
			assertThat( Hibernate.isPropertyInitialized( contained, "containingAsIndexedEmbeddedWithCast" ) ).isFalse();

			Object containingAsIndexedEmbeddedWithCast = contained.getContainingAsIndexedEmbeddedWithCast();
			assertThat( Hibernate.isPropertyInitialized( contained, "containingAsIndexedEmbeddedWithCast" ) ).isTrue();
			assertThat( containingAsIndexedEmbeddedWithCast ).isNotNull();
		} );

		scope.inTransaction( session -> {
			ContainedEntity contained = session.find( ContainedEntity.class, 4 );
			assertThat( contained.getContainingAsIndexedEmbeddedWithCast() ).isNotNull();
		} );
	}

	@Test
	void bothNotNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 1 );

			ContainedEntity contained1 = new ContainedEntity();
			contained1.setId( 4 );
			contained1.setContainingAsIndexedEmbeddedWithCast( containingEntity1 );
			containingEntity1.setContainedIndexedEmbeddedWithCast( contained1 );

			contained1.setContainingAsIndexedEmbedded( containingEntity1 );
			containingEntity1.setContainedIndexedEmbedded( contained1 );


			session.persist( contained1 );
			session.persist( containingEntity1 );

		} );

		scope.inTransaction( session -> {
			ContainedEntity contained = session.find( ContainedEntity.class, 4 );

			ContainingEntity containingAsIndexedEmbedded = contained.getContainingAsIndexedEmbedded();
			assertThat( containingAsIndexedEmbedded ).isNotNull();
			assertThat( Hibernate.isPropertyInitialized( contained, "containingAsIndexedEmbedded" ) ).isTrue();
			assertThat( Hibernate.isPropertyInitialized( contained, "containingAsIndexedEmbeddedWithCast" ) ).isFalse();

			Object containingAsIndexedEmbeddedWithCast = contained.getContainingAsIndexedEmbeddedWithCast();
			assertThat( Hibernate.isPropertyInitialized( contained, "containingAsIndexedEmbeddedWithCast" ) ).isTrue();
			assertThat( containingAsIndexedEmbeddedWithCast ).isNotNull();
		} );

		scope.inTransaction( session -> {
			ContainedEntity contained = session.find( ContainedEntity.class, 4 );
			assertThat( contained.getContainingAsIndexedEmbeddedWithCast() ).isNotNull();
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "containing")
	public static class ContainingEntity {

		@Id
		private Integer id;

		@OneToOne
		private ContainedEntity containedIndexedEmbedded;

		@OneToOne(targetEntity = ContainedEntity.class)
		@JoinColumn(name = "CIndexedEmbeddedCast")
		private ContainedEntity containedIndexedEmbeddedWithCast;


		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public ContainedEntity getContainedIndexedEmbedded() {
			return containedIndexedEmbedded;
		}

		public Object getContainedIndexedEmbeddedWithCast() {
			return containedIndexedEmbeddedWithCast;
		}

		public void setContainedIndexedEmbeddedWithCast(ContainedEntity containedIndexedEmbeddedWithCast) {
			this.containedIndexedEmbeddedWithCast = containedIndexedEmbeddedWithCast;
		}

		public void setContainedIndexedEmbedded(ContainedEntity containedIndexedEmbedded) {
			this.containedIndexedEmbedded = containedIndexedEmbedded;
		}
	}

	@Entity(name = "contained")
	public static class ContainedEntity {
		@Id
		private Integer id;

		@OneToOne(mappedBy = "containedIndexedEmbeddedWithCast", targetEntity = ContainingEntity.class,
				fetch = FetchType.LAZY)
		@LazyGroup("containingAsIndexedEmbeddedWithCast")
		private ContainingEntity containingAsIndexedEmbeddedWithCast;

		@OneToOne(mappedBy = "containedIndexedEmbedded", fetch = FetchType.LAZY)
		@LazyGroup("containingAsIndexedEmbedded")
		private ContainingEntity containingAsIndexedEmbedded;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public ContainingEntity getContainingAsIndexedEmbedded() {
			return containingAsIndexedEmbedded;
		}

		public Object getContainingAsIndexedEmbeddedWithCast() {
			return containingAsIndexedEmbeddedWithCast;
		}

		public void setContainingAsIndexedEmbeddedWithCast(ContainingEntity containingAsIndexedEmbeddedWithCast) {
			this.containingAsIndexedEmbeddedWithCast = containingAsIndexedEmbeddedWithCast;
		}

		public void setContainingAsIndexedEmbedded(ContainingEntity containingAsIndexedEmbedded) {
			this.containingAsIndexedEmbedded = containingAsIndexedEmbedded;
		}
	}
}
