/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.c3p0;

import java.sql.Statement;

import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests that checks the JDBC 4.2 compatibility of c3p0
 *
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect(value = SQLServerDialect.class)
@DomainModel(annotatedClasses = IrrelevantEntity.class)
@SessionFactory
public class JdbcCompatibilityTest {

	@Test
	@JiraKey(value = "HHH-11308")
	public void testJdbc41(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.doWork( connection -> {
				//Connection#getSchema was added in Java 1.7
				String schema = connection.getSchema();
				assertNotNull( schema );
			} );
		} );
	}

	@Test
	@JiraKey(value = "HHH-11308")
	public void testJdbc42(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int i = 0; i < 5; i++ ) {
				IrrelevantEntity entity = new IrrelevantEntity();
				entity.setName( getClass().getName() );
				session.persist( entity );
			}
			session.flush();
			session.doWork( connection -> {
				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( "DELETE FROM IrrelevantEntity" );
					assertEquals( 5, statement.getLargeUpdateCount() );
				}
			} );
		} );
	}

}
