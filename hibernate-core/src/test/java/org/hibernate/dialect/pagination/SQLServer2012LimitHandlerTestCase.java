/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.dialect.pagination;

import static org.junit.Assert.assertEquals;

import org.hibernate.engine.spi.RowSelection;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for the behavior of the SQLServer2012LimitHandler
 * 
 * @author Ivan Ermolaev
 */
public class SQLServer2012LimitHandlerTestCase extends BaseUnitTestCase {

	private RowSelection rowSelection;
	private SQLServer2012LimitHandler limitHandler;

	@Before
	public void setUp() {
		rowSelection = new RowSelection();
	}

	@Test
	public void getProcessedSqlWithFirstRow() {
		String sql = "select f1 from t1 order by f2";
		rowSelection.setFirstRow( 10 );
		rowSelection.setMaxRows( 5 );
		limitHandler = new SQLServer2012LimitHandler( sql, rowSelection );
		assertEquals( "select f1 from t1 order by f2 offset ? rows fetch next ? rows only", limitHandler.getProcessedSql() );
	}

	@Test
	public void getProcessedSqlWithoutFirstRow() {
		String sql = "select f1 from t1 order by f2";
		rowSelection.setMaxRows( 5 );
		limitHandler = new SQLServer2012LimitHandler( sql, rowSelection );
		assertEquals( "select f1 from t1 order by f2 offset 0 rows fetch next ? rows only", limitHandler.getProcessedSql() );
	}
}
