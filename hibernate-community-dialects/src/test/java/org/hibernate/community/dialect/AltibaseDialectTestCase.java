/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.orm.test.dialect.LimitQueryOptions;
import org.hibernate.query.spi.Limit;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Unit test of the behavior of the AltibaseDialect utility methods
 *
 * @author Geoffrey Park
 */
@BaseUnitTest
public class AltibaseDialectTestCase {
	private Dialect dialect;

	@BeforeEach
	public void setUp() {
		dialect = new AltibaseDialect( DatabaseVersion.make( 7, 3 ) );
	}

	@AfterEach
	public void tearDown() {
		dialect = null;
	}

	@Test
	public void testSupportLimits() {
		assertThat( dialect.getLimitHandler().supportsLimit() ).isTrue();
	}

	@Test
	public void testSelectWithLimitOnly() {
		assertThat( withLimit( "select c1, c2 from t1 order by c1, c2 desc",
				toRowSelection( 0, 15 ) ).toLowerCase( Locale.ROOT ) )
				.isEqualTo( "select c1, c2 from t1 order by c1, c2 desc limit ?" );
	}

	@Test
	public void testSelectWithOffsetLimit() {
		assertThat( withLimit( "select c1, c2 from t1 order by c1, c2 desc",
				toRowSelection( 5, 15 ) ).toLowerCase( Locale.ROOT ) )
				.isEqualTo( "select c1, c2 from t1 order by c1, c2 desc limit 1+?,?" );
	}

	@Test
	public void testSelectWithNoLimit() {
		assertThat( withLimit( "select c1, c2 from t1 order by c1, c2 desc", null ).toLowerCase( Locale.ROOT ) )
				.isEqualTo( "select c1, c2 from t1 order by c1, c2 desc" );
	}

	private String withLimit(String sql, Limit limit) {
		return dialect.getLimitHandler().processSql( sql, -1, null, new LimitQueryOptions( limit ) );
	}

	private Limit toRowSelection(int firstRow, int maxRows) {
		Limit selection = new Limit();
		selection.setFirstRow( firstRow );
		selection.setMaxRows( maxRows );
		return selection;
	}
}
