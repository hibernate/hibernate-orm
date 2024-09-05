/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.sql;


import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.Template;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel
public class TemplateTest {

	@Test
	@JiraKey("HHH-18256")
	public void templateLiterals(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();
		assertWhereStringTemplate( "N'a'", factory );
		assertWhereStringTemplate( "X'a'", factory );
		assertWhereStringTemplate( "BX'a'", factory);
		assertWhereStringTemplate( "VARBYTE'a'", factory );
		assertWhereStringTemplate( "bytea 'a'", factory );
		assertWhereStringTemplate( "bytea  'a'", factory );
		assertWhereStringTemplate( "date 'a'", factory );
		assertWhereStringTemplate( "time 'a'", factory );
		assertWhereStringTemplate( "timestamp 'a'", factory );
		assertWhereStringTemplate( "timestamp with time zone 'a'", factory );
		assertWhereStringTemplate( "time with time zone 'a'", factory );
		assertWhereStringTemplate( "date", "$PlaceHolder$.date", factory );
		assertWhereStringTemplate( "time", "$PlaceHolder$.time", factory );
		assertWhereStringTemplate( "zone", "$PlaceHolder$.zone", factory );
		assertWhereStringTemplate("select date from thetable",
				"select $PlaceHolder$.date from thetable", factory );
		assertWhereStringTemplate("select date '2000-12-1' from thetable",
				"select date '2000-12-1' from thetable", factory );
		assertWhereStringTemplate("where date between date '2000-12-1' and date '2002-12-2'",
				"where $PlaceHolder$.date between date '2000-12-1' and date '2002-12-2'", factory );
	}

	private static void assertWhereStringTemplate(String sql, SessionFactoryImplementor sf) {
		final String template = Template.renderWhereStringTemplate(
				sql,
				sf.getJdbcServices().getDialect(),
				sf.getTypeConfiguration(),
				sf.getQueryEngine().getSqmFunctionRegistry()
		);
		assertEquals( sql, template );
	}

	private static void assertWhereStringTemplate(String sql, String result, SessionFactoryImplementor sf) {
		final String template = Template.renderWhereStringTemplate(
				sql,
				sf.getJdbcServices().getDialect(),
				sf.getTypeConfiguration(),
				sf.getQueryEngine().getSqmFunctionRegistry()
		);
		assertEquals( result, template );
	}

}
