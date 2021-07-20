/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.rowid;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Version;

import org.hibernate.annotations.RowId;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Gavin King
 */
@RequiresDialect( value = Oracle9iDialect.class )
public class RowIdJpaTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
			Parent.class,
			Child.class,
		};
	}

	@Test
	public void testDeleteWithVersion() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Parent parent = new Parent();
			parent.id = 1L;
			parent.name = "John Doe";

			entityManager.persist( parent );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Parent parent = entityManager.getReference( Parent.class, 1L );
			entityManager.remove( parent );
		} );
	}

	@Test
	public void testOrphanRemoval() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Parent parent = new Parent();
			parent.id = 1L;
			parent.name = "John Doe";

			for ( long i = 1; i < 3; i++ ) {
				Child child = new Child();
				child.id = i;
				parent.addChild( child );
			}

			entityManager.persist( parent );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Parent parent = entityManager.find( Parent.class, 1L );
			parent.removeChild( parent.children.get( 0 ) );
		} );
	}

	@Test
	public void testCascadeDelete() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Parent parent = new Parent();
			parent.id = 1L;
			parent.name = "John Doe";

			for ( long i = 1; i < 3; i++ ) {
				Child child = new Child();
				child.id = i;
				parent.addChild( child );
			}

			entityManager.persist( parent );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Parent parent = entityManager.getReference( Parent.class, 1L );
			entityManager.remove( parent );
		} );
	}

	@Entity(name = "Parent" )
	@RowId( "ROWID" )
	public static class Parent {

		@Id
		private Long id;

		private String name;

		@Version
		private Long version;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<Child> children = new ArrayList<>(  );

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void addChild(Child child) {
			child.parent = this;
			children.add( child );
		}

		public void removeChild(Child child) {
			child.parent = null;
			children.remove( child );
		}
	}

	@Entity(name = "Child" )
	@RowId( "ROWID" )
	public static class Child {

		@Id
		private Long id;

		@ManyToOne
		private Parent parent;

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Child child = (Child) o;
			return Objects.equals( id, child.id );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id );
		}
	}

}

