/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.mapping;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class DefaultCascadeTest extends BaseEntityManagerFunctionalTestCase {

	@Test
	public void testCascadePersist() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Parent parent = new Parent();
			Child child = new Child();
			child.parent = parent;

			entityManager.persist( child );
		} );
	}

	@Override
	public String[] getEjb3DD() {
		return new String[] {
				"org/hibernate/jpa/test/mapping/orm.xml"
		};
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Parent.class,
			Child.class
		};
	}

	@Entity
	@Table(name = "Parent")
	public static class Parent {

		@Id
		@GeneratedValue
		private Integer id;
	}

	@Entity
	@Table(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue
		private Integer id;

		@ManyToOne
		private Parent parent;
	}
}
