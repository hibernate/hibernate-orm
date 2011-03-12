/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.test.sql.hand.custom.sqlserver;

import junit.framework.Test;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.test.sql.hand.custom.CustomStoredProcTestSupport;

/**
 * Custom SQL tests for SQLServer
 *
 * @author Gail Badner
 */
public class SQLServerCustomSQLTest extends CustomStoredProcTestSupport {

	public SQLServerCustomSQLTest(String str) {
		super( str );
	}

	public String[] getMappings() {
		return new String[] { "sql/hand/custom/sqlserver/Mappings.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( SQLServerCustomSQLTest.class );
	}

	public boolean appliesTo(Dialect dialect) {
		return ( dialect instanceof SQLServerDialect );
	}
}