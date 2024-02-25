package org.hibernate.orm.test.annotations.manytoone;

import org.hibernate.boot.beanvalidation.ValidationMode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-13959")
@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
public class NotNullManyToOneTest extends BaseCoreFunctionalTestCase {

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.JAKARTA_VALIDATION_MODE, ValidationMode.AUTO );
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

	@Test(expected = jakarta.validation.ConstraintViolationException.class)
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
