/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.formula;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.annotations.Formula;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Nathan Xu
 */
@DomainModel(
		annotatedClasses = {
				FormulaBasicsTest.Account.class
		}
)
@SessionFactory
public class FormulaBasicsTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Account account = new Account( );
			account.setId( 1L );
			account.setCredit( 5000d );
			account.setRate( 1.25 / 100 );
			session.persist( account );
		} );
	}

	@Test
	void testLoader(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Account account = session.find( Account.class, 1L );
			assertThat( account.getInterest(), is( 62.5d ));
		} );
	}

	@Test
	void testHQL(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Account account = session.createQuery( "select a from Account a where a.id = :id", Account.class )
					.setParameter( "id", 1L ).uniqueResult();
			assertThat( account.getInterest(), is( 62.5d ));
		} );
	}

	@Test
	void testHqlAmbiguous(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery( "select a, b from Account a, Account b" ).list();
		} );
	}

	@Test
	void testCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder criteriaBuilder = scope.getSessionFactory().getCriteriaBuilder();
			final CriteriaQuery<Account> criteria = criteriaBuilder.createQuery( Account.class );
			final Root<Account> root = criteria.from( Account.class );
			criteria.select( root );
			criteria.where( criteriaBuilder.equal( root.get( "id" ), criteriaBuilder.literal( 1L ) ) );
			final Account account = session.createQuery( criteria ).uniqueResult();
			assertThat( account.getInterest(), is( 62.5d ));
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "Account")
	public static class Account {

		@Id
		private Long id;

		private Double credit;

		private Double rate;

		@Formula(value = "credit * rate")
		private Double interest;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Double getCredit() {
			return credit;
		}

		public void setCredit(Double credit) {
			this.credit = credit;
		}

		public Double getRate() {
			return rate;
		}

		public void setRate(Double rate) {
			this.rate = rate;
		}

		public Double getInterest() {
			return interest;
		}

		public void setInterest(Double interest) {
			this.interest = interest;
		}
	}
}
