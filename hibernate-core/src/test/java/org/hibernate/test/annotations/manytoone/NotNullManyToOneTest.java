package org.hibernate.test.annotations.manytoone;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.beanvalidation.ValidationMode;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-13959")
public class NotNullManyToOneTest extends BaseCoreFunctionalTestCase {

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.JPA_VALIDATION_MODE, ValidationMode.AUTO.name() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Parent.class,
				Child.class
		};
	}

	@Test
	public void testSave() {
		inTransaction(
				session -> {
					Parent parent = new Parent( new Child() );
					session.save( parent );
				}
		);
	}

	@Test(expected = javax.validation.ConstraintViolationException.class)
	public void testSaveChildWithoutParent() {
		inTransaction(
				session -> {
					Child child = new Child();
					session.save( child );
				}
		);
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@NotNull
		@ManyToOne
		private Parent parent;

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@ManyToOne(cascade = CascadeType.ALL)
		private Child child;

		public Parent(Child child) {
			this.child = child;
			this.child.setParent( this );
		}

		public Child getChild() {
			return child;
		}
	}
}
