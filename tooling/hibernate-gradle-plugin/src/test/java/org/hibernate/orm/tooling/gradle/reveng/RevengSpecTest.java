/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.reveng;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class RevengSpecTest {

	@Test
	void testRevengSpec() {
		Map<String, Field> fieldMap = new HashMap<String, Field>();
		for(Field field : RevengSpec.class.getDeclaredFields()) {
			fieldMap.put(field.getName(), field);
		}
		assertNotNull(fieldMap.get("sqlToRun"));
	}

}
