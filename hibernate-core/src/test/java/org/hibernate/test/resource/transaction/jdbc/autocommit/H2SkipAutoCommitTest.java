/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.resource.transaction.jdbc.autocommit;

import java.util.HashMap;
import javax.sql.DataSource;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.test.util.ReflectionUtil;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
public class H2SkipAutoCommitTest extends AbstractSkipAutoCommitTest {

	public static void main(String... args) {
		HashMap map = new HashMap();
		map.put( "name", "first" );
		map.put( "name", "second" );
		System.out.printf( "name -> %s", map.get( "name" ) );
	}

	@Override
	protected DataSource dataSource() {
		final String url = Environment.getProperties().getProperty( AvailableSettings.URL );
		final String username = Environment.getProperties().getProperty( AvailableSettings.USER );
		final String password = Environment.getProperties().getProperty( AvailableSettings.PASS );

		final DataSource dataSource = ReflectionUtil.newInstance( "org.h2.jdbcx.JdbcDataSource" );
		injectValue( dataSource, "URL", url );
		injectValue( dataSource, "user", username );
		injectValue( dataSource, "password", password );
		return dataSource;
	}

	private void injectValue(DataSource dataSource, String name, String value) {
		if ( value == null ) {
			return;
		}
		ReflectionUtil.setProperty( dataSource, name, value );
	}
}
