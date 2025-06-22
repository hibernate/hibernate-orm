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

import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.Formula;

import org.hibernate.community.dialect.FirebirdDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
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
				FormulaTests.Account.class
		}
)
@SessionFactory
public class FormulaTests {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Account account = new Account( );
			account.setId( 1L );
			account.setCredit( 5000d );
			account.setRate( 1.25 / 100 );
			session.persist( account );
		} );
	}

	@Test
	void testLoader(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Account account = session.find( Account.class, 1L );
			assertThat( account.getInterest(), is( 62.5d ));
		} );
	}

	@Test
	void testHQL(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Account account = session.createQuery( "select a from Account a where a.id = :id", Account.class )
					.setParameter( "id", 1L ).uniqueResult();
			assertThat( account.getInterest(), is( 62.5d ));
		} );
	}

	@Test
	void testCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder criteriaBuilder = scope.getSessionFactory().getCriteriaBuilder();
			final CriteriaQuery<Account> criteria = criteriaBuilder.createQuery( Account.class );
			Root<Account> root = criteria.from( Account.class );
			criteria.select( root );
			criteria.where( criteriaBuilder.equal( root.get( "id" ), criteriaBuilder.literal( 1L ) ) );
			Account account = session.createQuery( criteria ).uniqueResult();
			assertThat( account.getInterest(), is( 62.5d ) );
			assertThat( account.getRatePercent(), is("1.25%") );
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

		@Formula(value = "(rate * 100) || '%'")
		@DialectOverride.Formula(dialect = MySQLDialect.class,
				override = @Formula("concat(rate * 100, '%')"))
		@DialectOverride.Formula(dialect = HSQLDialect.class,
				override = @Formula("replace(cast(rate * 100 as varchar(10)),'E0','') || '%'"))
		@DialectOverride.Formula(dialect = DerbyDialect.class,
				override = @Formula("trim(cast(cast(rate * 100 as decimal(10,2)) as char(10))) || '%'")) //LOL, Derby
		@DialectOverride.Formula(dialect = DB2Dialect.class,
				override = @Formula("varchar_format(rate * 100) || '%'"))
		@DialectOverride.Formula(dialect = OracleDialect.class,
				override = @Formula("to_char(cast(rate * 100 as number(10,2))) || '%'"))
		@DialectOverride.Formula(dialect = SQLServerDialect.class,
				override = @Formula("ltrim(str(rate * 100, 10, 2)) + '%'"))
		@DialectOverride.Formula(dialect = SybaseDialect.class,
				override = @Formula("ltrim(str(rate * 100, 10, 2)) + '%'"))
		@DialectOverride.Formula(dialect = FirebirdDialect.class,
				override = @Formula("cast(rate * 100 as decimal(10,2)) || '%'"))
		private String ratePercent;

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

		public String getRatePercent() {
			return ratePercent;
		}

		public void setRatePercent(String ratePercent) {
			this.ratePercent = ratePercent;
		}
	}
}
