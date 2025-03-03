/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loading;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				LoadParentChildEntityTest.ContainingEntity.class,
		}
)
@SessionFactory
public class LoadParentChildEntityTest {

	@Test
	public void testLoad(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ContainingEntity parent = new ContainingEntity();
					parent.setId( 1 );

					ContainingEntity child = new ContainingEntity();
					child.setId( 2 );

					parent.setChild( child );

					child.setParent( parent );

					session.persist( parent );
					session.persist( child );

					assertThat( parent.getChild() ).isNotNull();
				}
		);

		scope.inTransaction(
				session -> {
					ContainingEntity load = session.getReference( ContainingEntity.class, 1 );
					assertThat( load.getChild() ).isNotNull();
					assertThat( load.getParent() ).isNull();
				}
		);

		scope.inTransaction(
				session -> {
					ContainingEntity load = session.getReference( ContainingEntity.class, 2 );
					assertThat( load.getParent() ).isNotNull();
					assertThat( load.getChild() ).isNull();
				}
		);
	}


	@Entity(name = "containing")
	public static class ContainingEntity {

		@Id
		private Integer id;

		@OneToOne
		private ContainingEntity parent;

		@OneToOne(mappedBy = "parent")
		private ContainingEntity child;

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

	}
}
