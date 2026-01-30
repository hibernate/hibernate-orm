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

import java.util.List;

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
		assertWhereStringTemplate( "CAST(foo AS unsigned)",
				"CAST({@}.foo AS unsigned)", factory );
		assertWhereStringTemplate( "CAST(foo AS signed)",
				"CAST({@}.foo AS signed)", factory );
	}

	@Test
	@JiraKey("HHH-19695")
	public void testFetchGrammarVsColumnNames(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();

		// Test that "first" and "next" are treated as keywords when part of FETCH grammar
		assertWhereStringTemplate( "fetch first 10 rows only", "fetch first 10 rows only", factory );
		assertWhereStringTemplate( "fetch next 5 rows only", "fetch next 5 rows only", factory );
		assertWhereStringTemplate( "select * from table fetch first 1 row only",
				"select * from table fetch first 1 row only", factory );

		// Mixed scenarios: ensure identifiers around FETCH grammar are still qualified
		assertWhereStringTemplate( "select first_name from users fetch first 10 rows only",
				"select {@}.first_name from users fetch first 10 rows only", factory );
		assertWhereStringTemplate( "where fetch_count > 5 and fetch next 1 row only",
				"where {@}.fetch_count > 5 and fetch next 1 row only", factory );
		assertWhereStringTemplate( "select first from users fetch first 10 rows only",
				"select {@}.first from users fetch first 10 rows only", factory );
		assertWhereStringTemplate( "select next from users fetch next 10 rows only",
				"select {@}.next from users fetch next 10 rows only", factory );
	}

	@Test
	@JiraKey("HHH-19695")
	public void testFetchGrammarVariants(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();
		Dialect dialect = factory.getJdbcServices().getDialect();

		// Variants of FETCH FIRST/NEXT
		assertWhereStringTemplate( "fetch first 1 row only", "fetch first 1 row only", factory );
		assertWhereStringTemplate( "fetch next 10 rows only", "fetch next 10 rows only", factory );

		// Parameterized row count
		assertWhereStringTemplate( "fetch next ? rows only", "fetch next ? rows only", factory );

		// Casing variants
		assertWhereStringTemplate( "FETCH First 10 ROWS ONLY", "FETCH First 10 ROWS ONLY", factory );

		// Extra whitespace and newlines
		assertWhereStringTemplate( "fetch    first   10   rows   only", "fetch    first   10   rows   only", factory );
		assertWhereStringTemplate( "fetch\nfirst 3 rows only", "fetch\nfirst 3 rows only", factory );

		// State reset after ONLY: trailing 'next' should be qualified
		assertWhereStringTemplate( "fetch next 1 rows only and next > 5",
				"fetch next 1 rows only and {@}.next > 5", factory );

		// Qualified identifier should remain as-is
		assertWhereStringTemplate( "select u.first from users u fetch first 1 row only",
				"select u.first from users u fetch first 1 row only", factory );

		// Quoted identifier should be qualified, while FETCH clause remains unqualified
		assertWhereStringTemplate( "select `first` from users fetch first 1 row only",
				"select {@}." + dialect.quote("`first`") + " from users fetch first 1 row only", factory );
	}

	@Test
	@JiraKey("HHH-19704")
	public void testOrderedSetAggregationVariants(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();

		assertWhereStringTemplate(
				"SELECT LISTAGG(employee_name, ', ') WITHIN GROUP (ORDER BY hire_date) FROM employees",
				"SELECT LISTAGG({@}.employee_name, ', ') WITHIN GROUP (ORDER BY {@}.hire_date) FROM employees",
				factory );

		assertWhereStringTemplate(
				"SELECT PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY salary) FROM employees",
				"SELECT PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY {@}.salary) FROM employees",
				factory );

		assertWhereStringTemplate(
				"SELECT PERCENTILE_DISC(0.5) WITHIN GROUP (ORDER BY salary) FROM employees",
				"SELECT PERCENTILE_DISC(0.5) WITHIN GROUP (ORDER BY {@}.salary) FROM employees",
				factory );

		assertWhereStringTemplate(
				"SELECT MODE() WITHIN GROUP (ORDER BY job_title) FROM employees",
				"SELECT MODE() WITHIN GROUP (ORDER BY {@}.job_title) FROM employees",
				factory );
	}

	@Test
	@JiraKey("HHH-19704")
	public void testOrderedSetAggregationExtendedVariants(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();

		assertWhereStringTemplate(
				"""
						SELECT LISTAGG(employee_name, ', ') ON OVERFLOW ERROR WITHIN GROUP (ORDER BY hire_date)
						FROM employees""",
				"""
						SELECT LISTAGG({@}.employee_name, ', ') ON OVERFLOW ERROR WITHIN GROUP (ORDER BY {@}.hire_date)
						FROM employees""",
				factory );

		assertWhereStringTemplate(
				"""
						SELECT LISTAGG(employee_name, ', ') ON OVERFLOW TRUNCATE WITH COUNT WITHIN GROUP (ORDER BY hire_date)
						FROM employees""",
				"""
						SELECT LISTAGG({@}.employee_name, ', ') ON OVERFLOW TRUNCATE WITH COUNT WITHIN GROUP (ORDER BY {@}.hire_date)
						FROM employees""",
				factory );

		assertWhereStringTemplate(
				"""
						SELECT LISTAGG(employee_name, ', ') ON OVERFLOW TRUNCATE '...' WITH COUNT WITHIN GROUP (ORDER BY hire_date)
						FROM employees""",
				"""
						SELECT LISTAGG({@}.employee_name, ', ') ON OVERFLOW TRUNCATE '...' WITH COUNT WITHIN GROUP (ORDER BY {@}.hire_date)
						FROM employees""",
				factory );

		assertWhereStringTemplate(
				"""
						SELECT LISTAGG(employee_name, ', ') ON OVERFLOW TRUNCATE WITHOUT COUNT WITHIN GROUP (ORDER BY hire_date)
						FROM employees""",
				"""
						SELECT LISTAGG({@}.employee_name, ', ') ON OVERFLOW TRUNCATE WITHOUT COUNT WITHIN GROUP (ORDER BY {@}.hire_date)
						FROM employees""",
				factory );
	}

	@Test
	public void testRenderTransformerReadFragment() {
		// Test the renderTransformerReadFragment method
		String fragment = "SELECT name, age FROM users WHERE id = ?";
		String result = Template.renderTransformerReadFragment(fragment, "name", "age");
		assertEquals("SELECT {@}.name, {@}.age FROM users WHERE id = ?", result);

		// Test with no column names
		result = Template.renderTransformerReadFragment(fragment);
		assertEquals(fragment, result);

		// Test with empty column names array
		//noinspection RedundantArrayCreation
		result = Template.renderTransformerReadFragment(fragment, new String[0]);
		assertEquals(fragment, result);

		// Test with partial column name matches
		result = Template.renderTransformerReadFragment("SELECT name, age, address FROM users", "name");
		assertEquals("SELECT {@}.name, age, address FROM users", result);
	}

	@Test
	public void testCollectColumnNames(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();

		// Test collectColumnNames with SQL that gets processed
		List<String> columnNames = Template.collectColumnNames(
				"SELECT name, age FROM users WHERE id = ?",
				factory.getJdbcServices().getDialect(),
				factory.getTypeConfiguration()
		);
		assertEquals(3, columnNames.size()); // name, age, and id are unqualified identifiers

		// Test collectColumnNames with template that has qualified identifiers
		columnNames = Template.collectColumnNames("SELECT {@}.name, {@}.age FROM users WHERE {@}.id = ?");
		assertEquals(3, columnNames.size());
		assertEquals("name", columnNames.get(0));
		assertEquals("age", columnNames.get(1));
		assertEquals("id", columnNames.get(2));

		// Test with mixed qualified and unqualified identifiers
		columnNames = Template.collectColumnNames("SELECT {@}.name, age, {@}.address FROM users");
		assertEquals(2, columnNames.size());
		assertEquals("name", columnNames.get(0));
		assertEquals("address", columnNames.get(1));

		// Test with no template placeholders
		columnNames = Template.collectColumnNames("SELECT name, age FROM users");
		assertEquals(0, columnNames.size());

		// Test with template at end of string
		columnNames = Template.collectColumnNames("SELECT name FROM users WHERE id = {@}.value");
		assertEquals(1, columnNames.size());
		assertEquals("value", columnNames.get(0));
	}

	@Test
	public void testNamedParameters(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();

				// Test named parameters are not qualified
		assertWhereStringTemplate("SELECT * FROM users WHERE name = :name",
				"SELECT * FROM users WHERE {@}.name = :name", factory);
		assertWhereStringTemplate("SELECT * FROM users WHERE age > :minAge AND age < :maxAge",
				"SELECT * FROM users WHERE {@}.age > :minAge AND {@}.age < :maxAge", factory);

				// Test named parameters mixed with identifiers
		assertWhereStringTemplate("SELECT name FROM users WHERE id = :userId AND status = active",
				"SELECT {@}.name FROM users WHERE {@}.id = :userId AND {@}.status = {@}.active", factory);
	}

	@Test
	public void testBooleanLiterals(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();
		Dialect dialect = factory.getJdbcServices().getDialect();

				// Test boolean literals are converted to dialect-specific values
		assertWhereStringTemplate("SELECT * FROM users WHERE active = true",
				"SELECT * FROM users WHERE {@}.active = " + dialect.toBooleanValueString(true), factory);
		assertWhereStringTemplate("SELECT * FROM users WHERE deleted = false",
				"SELECT * FROM users WHERE {@}.deleted = " + dialect.toBooleanValueString(false), factory);

				// Test boolean literals in expressions
		assertWhereStringTemplate("SELECT * FROM users WHERE (active = true) AND (verified = false)",
				"SELECT * FROM users WHERE ({@}.active = " + dialect.toBooleanValueString(true) + ") AND ({@}.verified = " + dialect.toBooleanValueString(false) + ")", factory);

		// Test boolean literals mixed with identifiers
		assertWhereStringTemplate("SELECT name FROM users WHERE active = true AND status = pending",
				"SELECT {@}.name FROM users WHERE {@}.active = " + dialect.toBooleanValueString(true) + " AND {@}.status = {@}.pending", factory);
	}

	@Test
	public void testCurrentDateTimeExpressions(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();

				// Test CURRENT_DATE, CURRENT_TIME, CURRENT_TIMESTAMP expressions
		assertWhereStringTemplate("SELECT * FROM users WHERE created_date > CURRENT_DATE",
				"SELECT * FROM users WHERE {@}.created_date > CURRENT_DATE", factory);
		assertWhereStringTemplate("SELECT * FROM logs WHERE timestamp > CURRENT_TIMESTAMP",
				"SELECT * FROM logs WHERE timestamp > CURRENT_TIMESTAMP", factory);
		assertWhereStringTemplate("SELECT * FROM events WHERE event_time < CURRENT_TIME",
				"SELECT * FROM events WHERE {@}.event_time < CURRENT_TIME", factory);

		// Test current expressions in complex queries
		assertWhereStringTemplate("SELECT name FROM users WHERE last_login < CURRENT_TIMESTAMP - INTERVAL '1 day'",
				"SELECT {@}.name FROM users WHERE {@}.last_login < CURRENT_TIMESTAMP - INTERVAL '1 day'", factory);
	}

	@Test
	public void testFromClauseHandling(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();

		// Test that identifiers in FROM clause are not qualified
		assertWhereStringTemplate("SELECT name FROM users WHERE id = 1",
				"SELECT {@}.name FROM users WHERE {@}.id = 1", factory);
		assertWhereStringTemplate("SELECT u.name, o.order_id FROM users u JOIN orders o ON u.id = o.user_id",
				"SELECT u.name, o.order_id FROM users u JOIN orders o ON u.id = o.user_id", factory);

		// Test FROM clause with aliases
		assertWhereStringTemplate("SELECT name FROM users AS u WHERE u.id = 1",
				"SELECT {@}.name FROM users AS u WHERE u.id = 1", factory);

		// Test complex FROM clause with multiple tables
		assertWhereStringTemplate("SELECT p.name, c.name FROM products p, categories c WHERE p.category_id = c.id",
				"SELECT p.name, c.name FROM products p, categories c WHERE p.category_id = c.id", factory);
	}

	@Test
	public void testCastExpressions(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();

		// Test CAST expressions with type names
		assertWhereStringTemplate("SELECT CAST(price AS DECIMAL(10,2)) FROM products",
				"SELECT CAST({@}.price AS DECIMAL(10,2)) FROM products", factory);
		assertWhereStringTemplate("SELECT CAST(id AS VARCHAR) FROM users",
				"SELECT CAST({@}.id AS VARCHAR) FROM users", factory);

		// Test CAST with complex expressions
		assertWhereStringTemplate("SELECT CAST(price * 1.1 AS INTEGER) FROM products",
				"SELECT CAST({@}.price * 1.1 AS INTEGER) FROM products", factory);
	}

	@Test
	public void testFunctionCalls(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();

		// Test regular function calls
		assertWhereStringTemplate("SELECT UPPER(name) FROM users",
				"SELECT UPPER({@}.name) FROM users", factory);
		assertWhereStringTemplate("SELECT LOWER(email) FROM users",
				"SELECT LOWER({@}.email) FROM users", factory);
		assertWhereStringTemplate("SELECT COUNT(*) FROM users",
				"SELECT COUNT(*) FROM users", factory);

		// Test function calls with multiple parameters
		assertWhereStringTemplate("SELECT CONCAT(first_name, ' ', last_name) FROM users",
				"SELECT CONCAT({@}.first_name, ' ', {@}.last_name) FROM users", factory);

		// Test nested function calls
		assertWhereStringTemplate("SELECT UPPER(LOWER(name)) FROM users",
				"SELECT UPPER(LOWER({@}.name)) FROM users", factory);
	}

	@Test
	public void testExtractAndTrimFunctions(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();

		// Test EXTRACT function with FROM keyword
		assertWhereStringTemplate("SELECT EXTRACT(YEAR FROM created_date) FROM users",
				"SELECT EXTRACT(YEAR FROM {@}.created_date) FROM users", factory);
		assertWhereStringTemplate("SELECT EXTRACT(MONTH FROM birth_date) FROM users",
				"SELECT EXTRACT(MONTH FROM {@}.birth_date) FROM users", factory);

		// Test TRIM function with FROM keyword
		assertWhereStringTemplate("SELECT TRIM(LEADING ' ' FROM name) FROM users",
				"SELECT TRIM(LEADING ' ' FROM {@}.name) FROM users", factory);
		assertWhereStringTemplate("SELECT TRIM(TRAILING 'x' FROM code) FROM products",
				"SELECT TRIM(TRAILING 'x' FROM {@}.code) FROM products", factory);
	}

	@Test
	public void testQuotedIdentifiers(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();
		Dialect dialect = factory.getJdbcServices().getDialect();

		// Test backtick-quoted identifiers
		assertWhereStringTemplate("SELECT `user name` FROM users",
				"SELECT {@}." + dialect.quote("`user name`") + " FROM users", factory);
		assertWhereStringTemplate("SELECT `order-id` FROM orders",
				"SELECT {@}." + dialect.quote("`order-id`") + " FROM orders", factory);

				// Test dialect-specific quoted identifiers
		char openQuote = dialect.openQuote();
		char closeQuote = dialect.closeQuote();
		assertWhereStringTemplate("SELECT " + openQuote + "user name" + closeQuote + " FROM users",
				"SELECT {@}." + dialect.quote("`user name`") + " FROM users", factory);
	}

	@Test
	public void testQualifiedIdentifiers(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();

		// Test that already-qualified identifiers are not re-qualified
		assertWhereStringTemplate("SELECT u.name, o.order_id FROM users u JOIN orders o ON u.id = o.user_id",
				"SELECT u.name, o.order_id FROM users u JOIN orders o ON u.id = o.user_id", factory);

		// Test mixed qualified and unqualified identifiers
		assertWhereStringTemplate("SELECT u.name, status FROM users u WHERE u.id = 1",
				"SELECT u.name, {@}.status FROM users u WHERE u.id = 1", factory);

		// Test table-qualified identifiers
		assertWhereStringTemplate("SELECT users.name, users.email FROM users",
				"SELECT users.name, users.email FROM users", factory);
	}

	@Test
	public void testKeywordsAndIdentifiers(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();

		// Test that SQL keywords are not qualified
		assertWhereStringTemplate("SELECT * FROM users WHERE name IS NOT NULL",
				"SELECT * FROM users WHERE {@}.name IS NOT NULL", factory);
		assertWhereStringTemplate("SELECT DISTINCT name FROM users",
				"SELECT DISTINCT {@}.name FROM users", factory);
		assertWhereStringTemplate("SELECT name FROM users ORDER BY name",
				"SELECT {@}.name FROM users ORDER BY {@}.name", factory);

		// Test soft keywords that can be column names
		assertWhereStringTemplate("SELECT date, time FROM events",
				"SELECT {@}.date, {@}.time FROM events", factory);

		// Test soft keywords in CURRENT expressions
		assertWhereStringTemplate("SELECT * FROM events WHERE event_date > CURRENT_DATE",
				"SELECT * FROM events WHERE {@}.event_date > CURRENT_DATE", factory);
	}

	@Test
	public void testComplexExpressions(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();

		// Test complex expressions with multiple operators
		assertWhereStringTemplate("SELECT name FROM users WHERE age >= 18 AND age <= 65",
				"SELECT {@}.name FROM users WHERE {@}.age >= 18 AND {@}.age <= 65", factory);
		assertWhereStringTemplate("SELECT price FROM products WHERE price BETWEEN 10 AND 100",
				"SELECT {@}.price FROM products WHERE {@}.price BETWEEN 10 AND 100", factory);

		// Test expressions with parentheses
		assertWhereStringTemplate("SELECT name FROM users WHERE (age > 18) AND (status = 'active')",
				"SELECT {@}.name FROM users WHERE ({@}.age > 18) AND ({@}.status = 'active')", factory);

		// Test expressions with arithmetic operators
		assertWhereStringTemplate("SELECT name FROM products WHERE price * 1.1 > 100",
				"SELECT {@}.name FROM products WHERE {@}.price * 1.1 > 100", factory);
	}

	@Test
	public void testOrderedSetAggregationStateTransitions(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();

		// Test that state transitions work correctly after ordered-set aggregation
		assertWhereStringTemplate("SELECT LISTAGG(name, ', ') WITHIN GROUP (ORDER BY id) FROM users WHERE active = true",
				"SELECT LISTAGG({@}.name, ', ') WITHIN GROUP (ORDER BY {@}.id) FROM users WHERE {@}.active = true", factory);

		// Test that identifiers after GROUP are qualified again
		assertWhereStringTemplate("SELECT LISTAGG(name, ', ') WITHIN GROUP (ORDER BY id), status FROM users",
				"SELECT LISTAGG({@}.name, ', ') WITHIN GROUP (ORDER BY {@}.id), {@}.status FROM users", factory);

		// Test non-LISTAGG ordered-set aggregates
		assertWhereStringTemplate("SELECT PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY salary), department FROM employees",
				"SELECT PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY {@}.salary), {@}.department FROM employees", factory);
	}

	@Test
	public void testLookaheadFunctionality(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();

		// Test lookahead for function calls with whitespace
		assertWhereStringTemplate("SELECT UPPER (name) FROM users",
				"SELECT UPPER ({@}.name) FROM users", factory);
		assertWhereStringTemplate("SELECT COUNT ( * ) FROM users",
				"SELECT COUNT ( * ) FROM users", factory);

						// Test lookahead for CURRENT expressions with whitespace
		assertWhereStringTemplate("SELECT * FROM events WHERE timestamp > CURRENT TIMESTAMP",
				"SELECT * FROM events WHERE timestamp > CURRENT TIMESTAMP", factory);
		assertWhereStringTemplate("SELECT * FROM events WHERE date > CURRENT DATE",
				"SELECT * FROM events WHERE {@}.date > CURRENT DATE", factory);
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
