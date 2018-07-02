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
import org.hibernate.dialect.Oracle8iDialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.util.ReflectionUtil;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(Oracle8iDialect.class)
public class OracleSkipAutoCommitTest extends AbstractSkipAutoCommitTest {

	@Override
	protected DataSource dataSource() {
		DataSource dataSource = ReflectionUtil.newInstance( "oracle.jdbc.pool.OracleDataSource" );
		ReflectionUtil.setProperty( dataSource, "URL", Environment.getProperties().getProperty( AvailableSettings.URL ) );
		ReflectionUtil.setProperty( dataSource, "user", Environment.getProperties().getProperty( AvailableSettings.USER ) );
		ReflectionUtil.setProperty( dataSource, "password", Environment.getProperties().getProperty( AvailableSettings.PASS ) );

		return dataSource;
	}
}
