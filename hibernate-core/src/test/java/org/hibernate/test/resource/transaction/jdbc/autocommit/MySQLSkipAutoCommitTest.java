/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.resource.transaction.jdbc.autocommit;

import javax.sql.DataSource;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQL8Dialect;
import org.hibernate.dialect.MySQLDialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.util.ReflectionUtil;


/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(MySQLDialect.class)
public class MySQLSkipAutoCommitTest extends AbstractSkipAutoCommitTest {

	@Override
	protected DataSource dataSource() {
		DataSource dataSource = null;
		if ( getDialect() instanceof MariaDBDialect ) {
			dataSource = ReflectionUtil.newInstance( "org.mariadb.jdbc.MariaDbDataSource" );
		}
		else if ( getDialect() instanceof MySQLDialect ) {
			try {
				// ConnectorJ 8
				dataSource = ReflectionUtil.newInstance( "com.mysql.cj.jdbc.MysqlDataSource" );
			}
			catch (IllegalArgumentException e) {
				try {
					// ConnectorJ 5
					dataSource = ReflectionUtil.newInstance( "com.mysql.jdbc.jdbc2.optional.MysqlDataSource" );
				} catch (Exception e2) {
					e2.addSuppressed( e );
					throw e;
				}
			}
		}
		ReflectionUtil.setProperty( dataSource, "url", Environment.getProperties().getProperty( AvailableSettings.URL ) );
		ReflectionUtil.setProperty( dataSource, "user", Environment.getProperties().getProperty( AvailableSettings.USER ) );
		ReflectionUtil.setProperty( dataSource, "password", Environment.getProperties().getProperty( AvailableSettings.PASS ) );

		return dataSource;
	}
}
