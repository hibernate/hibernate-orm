/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.jpa.test.lock;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.LockModeType;
import javax.persistence.ManyToOne;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

@TestForIssue(jiraKey = "HHH-13000")
public class PessimisticWriteWithOptionalOuterJoinBreaksRefreshTest extends BaseEntityManagerFunctionalTestCase {

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long id;
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long id;

		@ManyToOne(cascade = { CascadeType.PERSIST })
		@JoinTable(name = "test")
		Parent parent;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Parent.class, Child.class };
	}

	private Child child;

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		doInJPA( this::entityManagerFactory, em -> {
			child = new Child();
			child.parent = new Parent();
			em.persist( child );
		} );
	}

	@Test
	public void pessimisticWriteWithOptionalOuterJoinBreaksRefreshTest() {
		doInJPA( this::entityManagerFactory, em -> {
			child = em.find( Child.class, child.id );
			em.lock( child, LockModeType.PESSIMISTIC_WRITE );
			em.flush();
			em.refresh( child );
		} );
	}

	@Test
	public void pessimisticReadWithOptionalOuterJoinBreaksRefreshTest() {
		doInJPA( this::entityManagerFactory, em -> {
			child = em.find( Child.class, child.id );
			em.lock( child, LockModeType.PESSIMISTIC_READ );
			em.flush();
			em.refresh( child );
		} );
	}

}
