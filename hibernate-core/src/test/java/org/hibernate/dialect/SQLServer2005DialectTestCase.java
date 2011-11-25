/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.dialect;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;

/**
 * Unit test of the behavior of the SQLServerDialect utility methods
 *
 * @author Valotasion Yoryos
 */
public class SQLServer2005DialectTestCase extends BaseUnitTestCase {

	@Test
	public void testStripAliases() {
		String input = "some_field1 as f1, some_fild2 as f2, _field3 as f3 ";

		assertEquals( "some_field1, some_fild2, _field3", SQLServer2005Dialect.stripAliases(input) );
	}

	@Test
	public void testGetSelectFieldsWithoutAliases() {
		StringBuilder input = new StringBuilder( "select some_field1 as f12, some_fild2 as f879, _field3 as _f24674_3 from...." );
		String output = SQLServer2005Dialect.getSelectFieldsWithoutAliases( input ).toString();

		assertEquals( " some_field1, some_fild2, _field3", output );
	}

	@Test
	public void testReplaceDistinctWithGroupBy() {
		StringBuilder input = new StringBuilder( "select distinct f1, f2 as ff, f3 from table where f1 = 5" );
		SQLServer2005Dialect.replaceDistinctWithGroupBy( input );

		assertEquals( "select f1, f2 as ff, f3 from table where f1 = 5 group by f1, f2, f3 ", input.toString() );
	}

	@Test
	public void testGetLimitString() {
		String input = "select distinct f1 as f53245 from table849752 order by f234, f67 desc";

		Dialect sqlDialect = new SQLServer2005Dialect();
		assertEquals( "with query as (select f1 as f53245, row_number() over (order by f234, f67 desc) as __hibernate_row_nr__ from table849752  group by f1) select * from query where __hibernate_row_nr__ >= ? and __hibernate_row_nr__ < ?", sqlDialect.getLimitString(input, 10, 15).toLowerCase() );
	}
}
