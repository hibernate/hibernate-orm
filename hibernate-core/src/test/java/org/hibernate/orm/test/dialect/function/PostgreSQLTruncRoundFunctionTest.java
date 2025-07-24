/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.function;

import java.math.BigDecimal;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.animal.Human;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marco Belladelli
 */
@DomainModel(standardModels = StandardDomainModel.ANIMAL)
@SessionFactory(statementInspectorClass = SQLStatementInspector.class)
@RequiresDialect(PostgreSQLDialect.class)
public class PostgreSQLTruncRoundFunctionTest {
	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@RequiresDialect(PostgreSQLDialect.class)
	@RequiresDialect(value = CockroachDialect.class, majorVersion = 22, minorVersion = 2, comment = "CockroachDB didn't support the two-argument trunc before version 22.2")
	public void testTrunc(SessionFactoryScope scope) {
		testFunction( scope, "trunc", "floor" );
	}

	@Test
	@RequiresDialect(PostgreSQLDialect.class)
	public void testRound(SessionFactoryScope scope) {
		testFunction( scope, "round", "floor" );
	}

	@Test
	@RequiresDialect(value = CockroachDialect.class, comment = "CockroachDB natively supports round with two args for both decimal and float types")
	public void testRoundWithoutWorkaround(SessionFactoryScope scope) {
		testFunction( scope, "round", "round" );
	}

	private void testFunction(SessionFactoryScope scope, String function, String workaround) {
		final SQLStatementInspector sqlStatementInspector = (SQLStatementInspector) scope.getStatementInspector();
		scope.inTransaction( session -> {
			Human human = new Human();
			human.setId( 1L );
			human.setHeightInches( 1.78253d );
			human.setFloatValue( 1.78253f );
			human.setBigDecimalValue( new BigDecimal( "1.78253" ) );
			session.persist( human );
		} );

		scope.inTransaction( session -> {
			sqlStatementInspector.clear();
			assertEquals(
					1.78d,
					session.createQuery(
							String.format( "select %s(h.heightInches, 2) from Human h", function ),
							Double.class
					).getSingleResult()
			);
			assertTrue( sqlStatementInspector.getSqlQueries().get( 0 ).contains( workaround ) );
			sqlStatementInspector.clear();
			assertEquals(
					1.78f,
					session.createQuery(
							String.format( "select %s(h.floatValue, 2) from Human h", function ),
							Float.class
					).getSingleResult()
			);
			assertTrue( sqlStatementInspector.getSqlQueries().get( 0 ).contains( workaround ) );
			sqlStatementInspector.clear();
			assertEquals(
					0,
					session.createQuery(
							String.format( "select %s(h.bigDecimalValue, 2) from Human h", function ),
							BigDecimal.class
					).getSingleResult().compareTo( new BigDecimal( "1.78" ) )
			);
			assertTrue( sqlStatementInspector.getSqlQueries().get( 0 ).contains( function ) );
		} );
	}
}
