/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql;


import org.hibernate.dialect.Dialect;
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
		Dialect dialect = factory.getJdbcServices().getDialect();
		assertWhereStringTemplate( "'Knock, knock! Who''s there?'",
				"'Knock, knock! Who''s there?'", factory );
		assertWhereStringTemplate( "1e-5 + 2 * 3.0",
				"1e-5 + 2 * 3.0", factory );
		assertWhereStringTemplate( "hello",
				"{@}.hello", factory );
		assertWhereStringTemplate( "`hello`",
				"{@}." + dialect.quote("`hello`"), factory );
		assertWhereStringTemplate( dialect.openQuote() + "hello" + dialect.closeQuote(),
				"{@}." + dialect.quote("`hello`"), factory );
		assertWhereStringTemplate( "hello.world",
				"hello.world", factory );
		assertWhereStringTemplate( "'hello there' || ' ' || 'world'",
				"'hello there' || ' ' || 'world'", factory );
		assertWhereStringTemplate( "hello + world",
				"{@}.hello + {@}.world", factory );
		assertWhereStringTemplate( "upper(hello) || lower(world)",
				"upper({@}.hello) || lower({@}.world)", factory );
		assertWhereStringTemplate( "extract(hour from time)",
				"extract(hour from {@}.time)", factory );
		assertWhereStringTemplate( "extract(day from date)",
				"extract(day from {@}.date)", factory );
		assertWhereStringTemplate( "left(hello,4) || right(world,5)",
				"left({@}.hello,4) || right({@}.world,5)", factory );
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
		assertWhereStringTemplate( "date", "{@}.date", factory );
		assertWhereStringTemplate( "time", "{@}.time", factory );
		assertWhereStringTemplate( "zone", "{@}.zone", factory );
		assertWhereStringTemplate("select date from thetable",
				"select {@}.date from thetable", factory );
		assertWhereStringTemplate("select date '2000-12-1' from thetable",
				"select date '2000-12-1' from thetable", factory );
		assertWhereStringTemplate("where date between date '2000-12-1' and date '2002-12-2'",
				"where {@}.date between date '2000-12-1' and date '2002-12-2'", factory );
		assertWhereStringTemplate("where foo>10 and bar is not null",
				"where {@}.foo>10 and {@}.bar is not null", factory );
		assertWhereStringTemplate("select t.foo, o.bar from table as t left join other as o on t.id = o.id where t.foo>10 and o.bar is not null order by o.bar",
				"select t.foo, o.bar from table as t left join other as o on t.id = o.id where t.foo>10 and o.bar is not null order by o.bar", factory );

	}

	private static void assertWhereStringTemplate(String sql, SessionFactoryImplementor sf) {
		assertEquals( sql,
				Template.renderWhereStringTemplate(
						sql,
						sf.getJdbcServices().getDialect(),
						sf.getTypeConfiguration()
				));
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
