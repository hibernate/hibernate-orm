/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.orm.test.dialect.LimitQueryOptions;
import org.hibernate.query.spi.Limit;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testing of patched support for Derby limit and offset queries; see HHH-3972
 *
 * @author Evan Leonard
 */
@BaseUnitTest
public class DerbyLegacyDialectTestCase {

	@Test
	@JiraKey(value = "HHH-3972")
	public void testInsertLimitClause() {
		final int limit = 50;
		final String input = "select * from tablename t where t.cat = 5";
		final String expected = "select * from tablename t where t.cat = 5 fetch first " + limit + " rows only";

		final String actual = withLimit( input, toRowSelection( 0, limit ) );
		assertThat( actual ).isEqualTo( expected );
	}

	@Test
	@JiraKey(value = "HHH-3972")
	public void testInsertLimitWithOffsetClause() {
		final int limit = 50;
		final int offset = 200;
		final String input = "select * from tablename t where t.cat = 5";
		final String expected = "select * from tablename t where t.cat = 5 offset " + offset + " rows fetch next " + limit + " rows only";

		final String actual = withLimit( input, toRowSelection( offset, limit ) );
		assertThat( actual ).isEqualTo( expected );
	}

	@Test
	@JiraKey(value = "HHH-3972")
	public void testInsertLimitWithForUpdateClause() {
		final int limit = 50;
		final int offset = 200;
		final String input = "select c11 as col1, c12 as col2, c13 as col13 from t1 for update of c11, c13";
		final String expected = "select c11 as col1, c12 as col2, c13 as col13 from t1 offset " + offset
								+ " rows fetch next " + limit + " rows only for update of c11, c13";

		final String actual = withLimit( input, toRowSelection( offset, limit ) );
		assertThat( actual ).isEqualTo( expected );
	}

	@Test
	@JiraKey(value = "HHH-3972")
	public void testInsertLimitWithWithClause() {
		final int limit = 50;
		final int offset = 200;
		final String input = "select c11 as col1, c12 as col2, c13 as col13 from t1 where flight_id between 'AA1111' and 'AA1112' with rr";
		final String expected = "select c11 as col1, c12 as col2, c13 as col13 from t1 where flight_id between 'AA1111' and 'AA1112' offset " + offset
								+ " rows fetch next " + limit + " rows only with rr";

		final String actual = withLimit( input, toRowSelection( offset, limit ) );
		assertThat( actual ).isEqualTo( expected );
	}

	@Test
	@JiraKey(value = "HHH-3972")
	public void testInsertLimitWithForUpdateAndWithClauses() {
		final int limit = 50;
		final int offset = 200;
		final String input = "select c11 as col1, c12 as col2, c13 as col13 from t1 where flight_id between 'AA1111' and 'AA1112' for update of c11,c13 with rr";
		final String expected = "select c11 as col1, c12 as col2, c13 as col13 from t1 where flight_id between 'AA1111' and 'AA1112' offset " + offset
								+ " rows fetch next " + limit + " rows only for update of c11,c13 with rr";

		final String actual = withLimit( input, toRowSelection( offset, limit ) );
		assertThat( actual ).isEqualTo( expected );
	}

	private String withLimit(String sql, Limit limit) {
		return new DerbyLegacyDialect( DatabaseVersion.make( 10, 5 ) ).getLimitHandler()
				.processSql( sql, -1, null, new LimitQueryOptions( limit ) );
	}

	private Limit toRowSelection(int firstRow, int maxRows) {
		Limit selection = new Limit();
		selection.setFirstRow( firstRow );
		selection.setMaxRows( maxRows );
		return selection;
	}
}
