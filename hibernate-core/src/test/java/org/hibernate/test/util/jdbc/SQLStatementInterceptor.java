package org.hibernate.test.util.jdbc;

import java.util.LinkedList;

import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * @author Vlad Mihalcea
 */
public class SQLStatementInterceptor {

	private final LinkedList<String> sqlQueries = new LinkedList<>( );

	public SQLStatementInterceptor(SessionFactoryBuilder sessionFactoryBuilder) {
		sessionFactoryBuilder.applyStatementInspector( (StatementInspector) sql -> {
			sqlQueries.add( sql );
			return sql;
		} );
	}

	public LinkedList<String> getSqlQueries() {
		return sqlQueries;
	}
}
