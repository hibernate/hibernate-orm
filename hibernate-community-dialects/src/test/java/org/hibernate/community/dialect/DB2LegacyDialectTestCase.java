/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.engine.jdbc.Size;
import org.hibernate.orm.test.dialect.LimitQueryOptions;
import org.hibernate.query.spi.Limit;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DB2 dialect related test cases
 *
 * @author Hardy Ferentschik
 */

@BaseUnitTest
public class DB2LegacyDialectTestCase {
	private final DB2LegacyDialect dialect = new DB2LegacyDialect();
	private TypeConfiguration typeConfiguration;

	@BeforeEach
	public void setup() {
		typeConfiguration = new TypeConfiguration();
		dialect.contributeTypes( () -> typeConfiguration, null );
	}

	@Test
	@JiraKey(value = "HHH-6866")
	public void testGetDefaultBinaryTypeName() {
		String actual = typeConfiguration.getDdlTypeRegistry().getTypeName( Types.BINARY, dialect );
		assertThat( actual )
				.describedAs( "The default column length is 255, but char length on DB2 is limited to 254" )
				.isEqualTo( "varchar($l) for bit data" );
	}

	@Test
	@JiraKey(value = "HHH-6866")
	public void testGetExplicitBinaryTypeName() {
		// lower bound
		String actual = typeConfiguration.getDdlTypeRegistry().getTypeName( Types.BINARY, Size.length( 1 ) );
		assertThat( actual )
				.describedAs( "Wrong binary type" )
				.isEqualTo( "char(1) for bit data" );

		// upper bound
		actual = typeConfiguration.getDdlTypeRegistry().getTypeName( Types.BINARY, Size.length( 254 ) );
		assertThat( actual )
				.describedAs( "Wrong binary type. 254 is the max length in DB2" )
				.isEqualTo( "char(254) for bit data" );

		// exceeding upper bound
		actual = typeConfiguration.getDdlTypeRegistry().getTypeName( Types.BINARY, Size.length( 255 ) );
		assertThat( actual )
				.describedAs( "Wrong binary type. Should be varchar for length > 254" )
				.isEqualTo( "varchar(255) for bit data" );
	}

	@Test
	@JiraKey(value = "HHH-12369")
	public void testIntegerOverflowForMaxResults() {
		Limit rowSelection = new Limit();
		rowSelection.setFirstRow( 1 );
		rowSelection.setMaxRows( Integer.MAX_VALUE );
		String sql = dialect.getLimitHandler().processSql( "select a.id from tbl_a a order by a.id", -1, null,
				new LimitQueryOptions( rowSelection ) );
		assertThat( sql ).describedAs( "Integer overflow for max rows in: " + sql )
				.contains( "fetch first 2147483647 rows only" );
	}
}
