/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.inheritance.joined;

import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author pholvs
 */
@DomainModel(
		annotatedClasses = {
				JoinedSubclassDuplicateFieldsWithTreatTest.Account.class,
				JoinedSubclassDuplicateFieldsWithTreatTest.Deposit.class,
				JoinedSubclassDuplicateFieldsWithTreatTest.Loan.class
		}
)
@SessionFactory
@JiraKey( "HHH-11686" )
public class JoinedSubclassDuplicateFieldsWithTreatTest {

	@Test
	public void queryConstrainedSubclass(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
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

		scope.inTransaction( (session) -> {
			List<Account> accounts = session
			.createQuery(
					"select a from Account a where treat(a as Loan).interest = 10",
					Account.class
			).getResultList();
			assertThat( accounts ).hasSize( 1 );
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
