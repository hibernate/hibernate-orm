/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.function;

import static java.util.Calendar.MONTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Richard H. Tingstad
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/dialect/function/Product.hbm.xml"
)
@SessionFactory
@RequiresDialect(value = SybaseASEDialect.class)
public class SybaseASEFunctionTest {

	@BeforeAll
	protected void prepareTest(SessionFactoryScope scope) throws Exception {
		scope.inTransaction(
				session -> {
					Product product = new Product();
					product.setPrice(new BigDecimal("0.5"));
					product.setDate( Calendar.getInstance().getTime() );
					session.persist( product );
				}
		);
	}

	@AfterAll
	protected void cleanupTest(SessionFactoryScope scope) throws Exception {
		scope.inTransaction(
				session -> session.createMutationQuery( "delete from Product" ).executeUpdate()
		);
	}

	@Test
	public void testCharLengthFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					var query = session.createQuery( "select char_length('123456') from Product", Number.class );
					assertEquals(6, query.uniqueResult().intValue());
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-7070")
	public void testDateaddFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					var query = session.createQuery( "select dateadd(day, 1, p.date) from Product p", Date.class );
					assertTrue(Calendar.getInstance().getTime().before(query.uniqueResult()));
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-7070")
	public void testDatepartFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					var query = session.createQuery( "select datepart(month, p.date) from Product p", Number.class );
					assertEquals(Calendar.getInstance().get(MONTH) + 1, query.uniqueResult().intValue());
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-7070")
	public void testDatediffFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					var query = session.createQuery( "select datediff(day, datetime 1999-07-19 00:00, datetime 1999-07-23 23:59) from Product", Number.class );
					assertEquals(4, query.uniqueResult().intValue());
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-7070")
	public void testAtn2Function(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					var query = session.createQuery("select atn2(p.price, .48) from Product p", Number.class);
					assertEquals(0.805803, query.uniqueResult().doubleValue(), 0.000001 );
				}
		);
	}
}
