/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.sql;


import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.sql.Template;
import org.hibernate.type.spi.TypeConfiguration;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(standardModels = StandardDomainModel.GAMBIT)
public class TemplateTest {

	@Test
	@JiraKey("HHH-18256")
	public void templateLiterals(SessionFactoryScope scope) {
		assertWhereStringTemplate( "N'a'", scope.getSessionFactory() );
		assertWhereStringTemplate( "X'a'", scope.getSessionFactory() );
		assertWhereStringTemplate( "BX'a'", scope.getSessionFactory() );
		assertWhereStringTemplate( "VARBYTE'a'", scope.getSessionFactory() );
		assertWhereStringTemplate( "bytea 'a'", scope.getSessionFactory() );
		assertWhereStringTemplate( "bytea  'a'", scope.getSessionFactory() );
		assertWhereStringTemplate( "date 'a'", scope.getSessionFactory() );
		assertWhereStringTemplate( "time 'a'", scope.getSessionFactory() );
		assertWhereStringTemplate( "timestamp 'a'", scope.getSessionFactory() );
		assertWhereStringTemplate( "timestamp with time zone 'a'", scope.getSessionFactory() );
		assertWhereStringTemplate( "time with time zone 'a'", scope.getSessionFactory() );
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

}
