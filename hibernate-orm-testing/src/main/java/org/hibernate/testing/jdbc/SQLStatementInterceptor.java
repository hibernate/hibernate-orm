/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.jdbc;

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
