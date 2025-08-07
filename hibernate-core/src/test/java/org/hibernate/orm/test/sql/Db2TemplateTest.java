/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql;


import org.hibernate.dialect.DB2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.Template;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel
@RequiresDialect( DB2Dialect.class )
public class Db2TemplateTest {

	@Test
	@JiraKey("HHH-19695")
	public void templateLiterals(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();

		// Test that dialect-specific keywords are NOT treated as column names
		// These should remain as-is, not prefixed with table alias

		// Test DB2-specific keywords first, next
		assertWhereStringTemplate( "fetch first 10 rows only", "fetch first 10 rows only", factory );
		assertWhereStringTemplate( "fetch next 5 rows only", "fetch next 5 rows only", factory );
		assertWhereStringTemplate( "select * from table fetch first 1 row only","select * from table fetch first 1 row only", factory );

		// Test that regular column names are still prefixed
		assertWhereStringTemplate( "first_name", "{@}.first_name", factory );
		assertWhereStringTemplate( "fetch_count", "{@}.fetch_count", factory );
		assertWhereStringTemplate( "rows_processed", "{@}.rows_processed", factory );
		assertWhereStringTemplate( "only_flag", "{@}.only_flag", factory );

		// Test mixed scenarios where keywords and column names appear together
		assertWhereStringTemplate( "select first_name from users fetch first 10 rows only",
				"select {@}.first_name from users fetch first 10 rows only", factory );
		assertWhereStringTemplate( "where fetch_count > 5 and fetch first 1 row only",
				"where {@}.fetch_count > 5 and fetch first 1 row only", factory );
	}

	private static void assertWhereStringTemplate(String sql, String result, SessionFactoryImplementor factory) {
		assertEquals( result,
				Template.renderWhereStringTemplate(
						sql,
						factory.getJdbcServices().getDialect(),
						factory.getTypeConfiguration()
				) );
	}

}
