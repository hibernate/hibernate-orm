/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Check if IndexQueryHintHandler handles correctly simple query and query with JOIN
 *
 * @author Rguihard
 */
class DialectTest {

	static Stream<Arguments> _addQueryHints() {
		final Stream.Builder<Arguments> builder = Stream.builder();

		final String hints = "MY_INDEX";
		final String simpleQuery = "select COUNT(*) from TEST t1_0 where column1 = 'value'";
		builder.add(
			Arguments.of("Simple query : hint",
				"select COUNT(*) from TEST t1_0  use index (MY_INDEX) where column1 = 'value'", simpleQuery, hints));
		final String joinQueryUsing = "select COUNT(*) from TEST t1_0 join TEST2 t2_0 using(column2) where field = 'value'";
		builder.add(Arguments.of("Join query with using : hint",
			"select COUNT(*) from TEST t1_0  use index (MY_INDEX) join TEST2 t2_0 using(column2) where field = 'value'",
			joinQueryUsing, hints));
		final String joinQueryOn = "select COUNT(*) from TEST t1_0 join TEST2 t2_0 on t1_0.column2 = t2_0.column2 where field = 'value'";
		builder.add(Arguments.of("Join query with on : hint",
			"select COUNT(*) from TEST t1_0  use index (MY_INDEX) join TEST2 t2_0 on t1_0.column2 = t2_0.column2 where field = 'value'",
			joinQueryOn, hints));
		final String leftJoinQuery = "select COUNT(*) from TEST t1_0 left join TEST2 t2_0 on t1_0.column2 = t2_0.column2 and t1_0.column3 = t2_0.column3 where field = 'value'";
		builder.add(Arguments.of("Left join query with on : hint",
			"select COUNT(*) from TEST t1_0  use index (MY_INDEX) left join TEST2 t2_0 on t1_0.column2 = t2_0.column2 and t1_0.column3 = t2_0.column3 where field = 'value'",
			leftJoinQuery, hints));

		return builder.build();
	}

	@MethodSource("_addQueryHints")
	@ParameterizedTest
	void addQueryHints(String description, String expected, String query, String hints) {
		final String queryWithHint = MySQLDialect.addUseIndexQueryHint(query, hints);
		assertEquals(expected, queryWithHint, description);
	}

}
