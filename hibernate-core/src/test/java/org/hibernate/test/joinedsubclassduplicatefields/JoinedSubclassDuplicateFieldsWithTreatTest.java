/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.joinedsubclassduplicatefields;

import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author pholvs
 */
@TestForIssue( jiraKey = "HHH-11686" )
public class JoinedSubclassDuplicateFieldsWithTreatTest extends BaseCoreFunctionalTestCase {
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
			Account.class,
			Deposit.class,
			Loan.class
		};
	}

	@Test
	@FailureExpected( jiraKey = "HHH-11686" )
	public void queryConstrainedSubclass() {
		doInHibernate( this::sessionFactory, session -> {
			Deposit deposit1 = new Deposit();
			deposit1.id = 1L;
			deposit1.interest = 10;

			Loan loan1 = new Loan();
			loan1.id = 2L;
			loan1.interest = 10;

			Deposit deposit2 = new Deposit();
			deposit2.id = 3L;
			deposit2.interest = 20;

			Loan loan2 = new Loan();
			loan2.id = 4L;
			loan2.interest = 30;

			session.persist(deposit1);
			session.persist(loan1);
			session.persist(deposit2);
			session.persist(loan2);
		} );

		doInHibernate( this::sessionFactory, session -> {
			List<Account> accounts = session
			.createQuery(
				"select a " +
				"from Account a " +
				"where treat(a as Loan).interest = 10")
			.getResultList();
			assertEquals(1, accounts.size());
		} );
	}

	@Entity(name = "Account")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Account
	{
		@Id
		public Long id;
	}


	@Entity(name = "Deposit")
	public static class Deposit extends Account {
		@Column
		public Integer interest;
	}

	@Entity(name = "Loan")
	public static class Loan extends Account {
		@Column
		public Integer interest;

		@Column
		public Integer rate;
	}
}

