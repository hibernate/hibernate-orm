/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.sql.Types;

import org.hibernate.engine.jdbc.Size;
import org.hibernate.query.spi.Limit;
import org.hibernate.type.spi.TypeConfiguration;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * DB2 dialect related test cases
 *
 * @author Hardy Ferentschik
 */

public class DB2LegacyDialectTestCase extends BaseUnitTestCase {
	private final DB2LegacyDialect dialect = new DB2LegacyDialect();
	private TypeConfiguration typeConfiguration;

	@Before
	public void setup() {
		typeConfiguration = new TypeConfiguration();
		dialect.contributeTypes( () -> typeConfiguration, null );
	}

	@Test
	@JiraKey(value = "HHH-6866")
	public void testGetDefaultBinaryTypeName() {
		String actual = typeConfiguration.getDdlTypeRegistry().getTypeName( Types.BINARY, dialect );
		assertEquals(
				"The default column length is 255, but char length on DB2 is limited to 254",
				"varchar($l) for bit data",
				actual
		);
	}

	@Test
	@JiraKey(value = "HHH-6866")
	public void testGetExplicitBinaryTypeName() {
		// lower bound
		String actual = typeConfiguration.getDdlTypeRegistry().getTypeName( Types.BINARY, Size.length( 1) );
		assertEquals(
				"Wrong binary type",
				"char(1) for bit data",
				actual
		);

		// upper bound
		actual = typeConfiguration.getDdlTypeRegistry().getTypeName( Types.BINARY, Size.length( 254) );
		assertEquals(
				"Wrong binary type. 254 is the max length in DB2",
				"char(254) for bit data",
				actual
		);

		// exceeding upper bound
		actual = typeConfiguration.getDdlTypeRegistry().getTypeName( Types.BINARY, Size.length( 255) );
		assertEquals(
				"Wrong binary type. Should be varchar for length > 254",
				"varchar(255) for bit data",
				actual
		);
	}

	@Test
	@JiraKey(value = "HHH-12369")
	public void testIntegerOverflowForMaxResults() {
		Limit rowSelection = new Limit();
		rowSelection.setFirstRow(1);
		rowSelection.setMaxRows(Integer.MAX_VALUE);
		String sql = dialect.getLimitHandler().processSql( "select a.id from tbl_a a order by a.id", rowSelection );
		assertTrue(
				"Integer overflow for max rows in: " + sql,
				sql.contains("fetch first 2147483647 rows only")
		);
	}
}
