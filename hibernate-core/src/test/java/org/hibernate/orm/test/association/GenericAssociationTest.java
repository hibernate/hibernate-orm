package org.hibernate.orm.test.association;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;

@TestForIssue(jiraKey = "HHH-16378")
public class GenericAssociationTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Parent.class,
				AbstractChild.class,
				Child.class
		};
	}

	@Test
	public void testFindByParentId() {
		inTransaction( session -> {
			Parent parent = new Parent( 1L );
			Child child = new Child( 2L );
			child.setParent( parent );
			session.persist( parent );
			session.persist( child );
		} );

		inTransaction( session -> {
			assertThat( session.createQuery( "from Child where parent.id = :parentId", Child.class )
					.setParameter( "parentId", 1L )
					.list() )
					.containsExactly( session.getReference( Child.class, 2L ) );
		} );
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		private Long id;

		public Parent() {
		}

		public Parent(Long id) {
			this.id = id;
		}

		public Long getId() {
			return this.id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@MappedSuperclass
	public abstract static class AbstractChild<T> {
		@OneToOne(optional = false)
		private T parent;

		public AbstractChild() {
		}

		public abstract Long getId();

		public T getParent() {
			return this.parent;
		}

		public void setParent(T parent) {
			this.parent = parent;
		}
	}

	@Entity(name = "Child")
	public static class Child extends AbstractChild<Parent> {
		@Id
		protected Long id;

		public Child() {
		}

		public Child(Long id) {
			this.id = id;
		}

		@Override
		public Long getId() {
			return this.id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}
