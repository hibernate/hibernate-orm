/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.descriptor.jdbc;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.BigIntJdbcType;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

public class ArrayJdbcTypeTest {
	@Test
	@JiraKey(value = "HHH-17662")
	public void testEquality() {
		Map<JdbcType, String> typeMap = new HashMap<>();
		JdbcType bigInt = new BigIntJdbcType();
		typeMap.put(new ArrayJdbcType(bigInt), "bees");
		typeMap.put(new ArrayJdbcType(bigInt), "bees");
		typeMap.put(new ArrayJdbcType(bigInt), "bees");
		typeMap.put(new ArrayJdbcType(new IntegerJdbcType()), "waffles");
		assertThat("A map of arrays only contains non duplicate entries", typeMap.size() == 2);
	}
}
