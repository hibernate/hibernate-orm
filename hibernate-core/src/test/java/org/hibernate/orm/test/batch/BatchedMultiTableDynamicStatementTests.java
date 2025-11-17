/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import java.util.List;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import static jakarta.persistence.InheritanceType.JOINED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE;

/**
 * @author Steve Ebersole
 */
public class BatchedMultiTableDynamicStatementTests {

	@Test
	@ServiceRegistry( settings = @Setting( name = STATEMENT_BATCH_SIZE, value = "2" ) )
	@DomainModel( annotatedClasses = { Payment.class, CheckPayment.class } )
	@SessionFactory( useCollectingStatementInspector = true )
	public void testBatched(SessionFactoryScope scope) {
		final SQLStatementInspector statementCollector = scope.getCollectingStatementInspector();
		statementCollector.clear();

		createData( scope );

		assertThat( statementCollector.getSqlQueries() ).hasSize( 6 );

		scope.inTransaction( (session) -> {
			final List<Payment> payments = session.createSelectionQuery( "from Payment", Payment.class ).list();
			assertThat( payments ).hasSize( 3 );
		} );
	}

	@Test
	@ServiceRegistry( settings = @Setting( name = STATEMENT_BATCH_SIZE, value = "-1" ) )
	@DomainModel( annotatedClasses = { Payment.class, CheckPayment.class } )
	@SessionFactory( useCollectingStatementInspector = true )
	public void testNonBatched(SessionFactoryScope scope) {
		final SQLStatementInspector statementCollector = scope.getCollectingStatementInspector();
		statementCollector.clear();

		createData( scope );

		assertThat( statementCollector.getSqlQueries() ).hasSize( 6 );

		scope.inTransaction( (session) -> {
			final List<Payment> payments = session.createSelectionQuery( "from Payment", Payment.class ).list();
			assertThat( payments ).hasSize( 3 );
		} );
	}

	private static void createData(SessionFactoryScope scope) {
		final CheckPayment payment = new CheckPayment();
		payment.setId( 1 );
		payment.setAmount( 123.00 );
		payment.setRoute( "0123-45-6789" );
		payment.setAccount( "0089654321" );

		final CheckPayment payment2 = new CheckPayment();
		payment2.setId( 2 );
		payment2.setAmount( 230.00 );
		payment2.setRoute( "0123-45-6789" );
		payment2.setAccount( "0089654321" );
		payment2.setMemo( "Car Loan" );

		final CheckPayment payment3 = new CheckPayment();
		payment3.setId( 3 );
		payment3.setAmount( 1234.00 );
		payment3.setRoute( "0123-45-6789" );
		payment3.setAccount( "0089654321" );

		scope.inTransaction( (session) -> {
			session.persist( payment );
			session.persist( payment2 );
			session.persist( payment3 );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "Payment" )
	@Table( name = "payments" )
	@Inheritance( strategy = JOINED )
	@DynamicInsert @DynamicUpdate
	public static class Payment {
		@Id
		private Integer id;
		@Column( name = "amt")
		private double amount;
		@Column( name = "the_comment")
		private String comment;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public double getAmount() {
			return amount;
		}

		public void setAmount(double amount) {
			this.amount = amount;
		}

		public String getComment() {
			return comment;
		}

		public void setComment(String comment) {
			this.comment = comment;
		}
	}

	@Entity( name = "CheckPayment")
	@Table( name = "check_payments" )
	@PrimaryKeyJoinColumn( name = "payment_fk" )
	@DynamicInsert
	@DynamicUpdate
	public static class CheckPayment extends Payment {
		@Basic(optional = false)
		private String route;
		@Basic(optional = false)
		@Column( name = "acct" )
		private String account;
		private String memo;

		public String getRoute() {
			return route;
		}

		public void setRoute(String route) {
			this.route = route;
		}

		public String getAccount() {
			return account;
		}

		public void setAccount(String account) {
			this.account = account;
		}

		public String getMemo() {
			return memo;
		}

		public void setMemo(String memo) {
			this.memo = memo;
		}
	}
}
