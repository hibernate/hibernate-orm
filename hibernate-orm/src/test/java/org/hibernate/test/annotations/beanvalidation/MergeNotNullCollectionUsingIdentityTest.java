/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.beanvalidation;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.ConstraintViolationException;
import javax.validation.constraints.NotNull;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Ryan Emerson
 */
@RequiresDialectFeature( value = DialectChecks.SupportsIdentityColumns.class, jiraKey = "HHH-9979")
@TestForIssue( jiraKey = "HHH-9979")
public class MergeNotNullCollectionUsingIdentityTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {Parent.class, Child.class};
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "javax.persistence.validation.mode", "AUTO" );
	}

	@Test
	@FailureExpected(jiraKey = "HHH-9979")
	public void testOneToManyNotNullCollection() {
		Parent parent = new Parent();
		Child child = new Child();

		List<Child> children = new ArrayList<Child>();
		children.add( child );

		child.setParent( parent );
		parent.setChildren( children );

		Session s = openSession();
		Transaction t = s.beginTransaction();
		parent = (Parent) s.merge( parent );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.delete( parent );
		t.commit();
		s.close();
	}

	@Test(expected = ConstraintViolationException.class)
	public void testOneToManyNullCollection() {
		Parent parent = new Parent();
		Child child = new Child();
		child.setParent( parent );

		Session s = openSession();
		Transaction t = s.beginTransaction();
		parent = (Parent) s.merge( parent );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.delete( parent );
		t.commit();
		s.close();
	}

	@Entity
	@Table(name = "PARENT")
	static class Parent {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
		@NotNull
		private List<Child> children;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void setChildren(List<Child> children) {
			this.children = children;
		}
	}

	@Entity
	@Table(name = "CHILD")
	static class Child {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@ManyToOne
		private Parent parent;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}
}
