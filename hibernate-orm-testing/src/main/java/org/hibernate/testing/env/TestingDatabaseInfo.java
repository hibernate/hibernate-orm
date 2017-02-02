/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.env;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;

/**
 * @author Steve Ebersole
 */
public final class TestingDatabaseInfo {
	public static volatile String DRIVER = "org.h2.Driver";
	public static volatile String URL = "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;MVCC=TRUE";
	public static volatile String USER = "sa";
	public static volatile String PASS = "";

	public static final Dialect DIALECT = new H2Dialect();

	public static Configuration buildBaseConfiguration() {
		return new Configuration()
				.setProperty( Environment.DRIVER, DRIVER )
				.setProperty( Environment.URL, URL )
				.setProperty( Environment.USER, USER )
				.setProperty( Environment.PASS, PASS )
				.setProperty( Environment.DIALECT, DIALECT.getClass().getName() );
	}

	private TestingDatabaseInfo() {
	}
}
