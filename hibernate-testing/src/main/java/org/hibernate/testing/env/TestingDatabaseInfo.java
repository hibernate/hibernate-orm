/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
