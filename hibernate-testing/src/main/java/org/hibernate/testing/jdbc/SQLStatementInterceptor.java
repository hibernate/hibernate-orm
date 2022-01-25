/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.jdbc;

import java.util.LinkedList;
import java.util.Map;

import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 * @deprecated use {@link SQLStatementInspector} instead
 */
@Deprecated
public class SQLStatementInterceptor {

	private final LinkedList<String> sqlQueries = new LinkedList<>();

	public SQLStatementInterceptor(SessionFactoryBuilder sessionFactoryBuilder) {
		sessionFactoryBuilder.applyStatementInspector( (StatementInspector) sql -> {
			sqlQueries.add( sql );
			return sql;
		} );
	}

	public SQLStatementInterceptor(Map<String,Object> settings) {
		settings.put( AvailableSettings.STATEMENT_INSPECTOR, (StatementInspector) sql -> {
			sqlQueries.add( sql );
			return sql;
		} );
	}

	public SQLStatementInterceptor(StandardServiceRegistryBuilder ssrb) {
		ssrb.applySetting(
				AvailableSettings.STATEMENT_INSPECTOR,
				(StatementInspector) sql -> {
					sqlQueries.add( sql );
					return sql;
				}
		);
	}

	public SQLStatementInterceptor(Configuration configuration) {
		this( PropertiesHelper.map( configuration.getProperties() ) );
	}

	public LinkedList<String> getSqlQueries() {
		return sqlQueries;
	}

	public void clear() {
		sqlQueries.clear();
	}

	public void assertExecuted(String expected) {
		assertTrue(sqlQueries.contains( expected ));
	}

	public void assertExecutedCount(int expected) {
		assertEquals(expected, sqlQueries.size());
	}

	public int getQueryCount() {
		return sqlQueries.size();
	}
}
