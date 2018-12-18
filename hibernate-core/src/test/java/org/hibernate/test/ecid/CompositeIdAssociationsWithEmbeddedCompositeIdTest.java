/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ecid;

import java.io.Serializable;
import java.util.Iterator;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class CompositeIdAssociationsWithEmbeddedCompositeIdTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Parent.class, Person.class };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-13114")
	public void testQueries() {
		Parent parent1 = new Parent( "Jane", 0 );
		Parent parent2 = new Parent( "Jim", 1 );
		Person person = doInHibernate(
				this::sessionFactory, session -> {
					Person p = new Person();
					p.setParent1( parent1 );
					p.setParent2( parent2 );
					p.setBirthOrder( 0 );
					session.persist( parent1 );
					session.persist( parent2 );
					session.persist( p );
					return p;
		});

		doInHibernate(
				this::sessionFactory, session -> {
					checkResult( session.get( Person.class, person ) );
		});


		doInHibernate(
				this::sessionFactory, session -> {
					checkResult( session.createQuery( "from Person p", Person.class ).getSingleResult() );
		});

		doInHibernate(
				this::sessionFactory, session -> {
					Iterator<Person> iterator = session.createQuery( "from Person p", Person.class ).iterate();
					assertTrue( iterator.hasNext() );
					Person p = iterator.next();
					checkResult( p );
					assertFalse( iterator.hasNext() );
		});
	}

	private void checkResult(Person p) {
		assertEquals( "Jane", p.getParent1().name );
		assertEquals( 0, p.getParent1().index );
		assertEquals( "Jim", p.getParent2().name );
		assertEquals( 1, p.getParent2().index );
	}

	@Entity(name = "Person")
	public static class Person implements Serializable {
		@Id
		@JoinColumns( value = {
				@JoinColumn(name = "p1Name"),
				@JoinColumn(name = "p1Index")
		})
		@ManyToOne
		private Parent parent1;

		@Id
		@JoinColumns( value = {
				@JoinColumn(name = "p2Name"),
				@JoinColumn(name = "p2Index")
		})
		@ManyToOne
		private Parent parent2;

		@Id
		private int birthOrder;

		private String name;

		public Person() {
		}

		public Person(String name, Parent parent1, Parent parent2) {
			this();
			setName( name );
			this.parent1 = parent1;
			this.parent2 = parent2;
		}

		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}

		public Parent getParent1() {
			return parent1;
		}
		public void setParent1(Parent parent1) {
			this.parent1 = parent1;
		}

		public Parent getParent2() {
			return parent2;
		}
		public void setParent2(Parent parent2) {
			this.parent2 = parent2;
		}

		public int getBirthOrder() {
			return birthOrder;
		}
		public void setBirthOrder(int birthOrder) {
			this.birthOrder = birthOrder;
		}
	}

	@Entity(name = "Parent")
	public static class Parent implements Serializable {
		@Id
		private String name;

		@Id
		@Column(name="ind")
		private int index;

		public Parent() {
		}

		public Parent(String name, int index) {
			this.name = name;
			this.index = index;
		}
	}
}