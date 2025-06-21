/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test;

import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.util.StringUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class StringUtilTest {
	@Test
	public void testIsPropertyName() {
		assertTrue( StringUtil.isProperty( "getFoo", "java.lang.Object" ) );
		assertTrue( StringUtil.isProperty( "isFoo", "Boolean" ) );
		assertTrue( StringUtil.isProperty( "hasFoo", "java.lang.Boolean" ) );

		assertFalse( StringUtil.isProperty( "isfoo", "void" ) );
		assertFalse( StringUtil.isProperty( "hasfoo", "java.lang.Object" ) );

		assertFalse( StringUtil.isProperty( "", "java.lang.Object" ) );
		assertFalse( StringUtil.isProperty( null, "java.lang.Object" ) );
	}

	@Test
	@TestForIssue(jiraKey = "METAGEN-76")
	public void testHashCodeNotAProperty() {
		assertFalse( StringUtil.isProperty( "hashCode", "Integer" ) );
	}

	@Test
	public void testGetUpperUnderscoreCaseFromLowerCamelCase(){
		assertEquals("USER_PARENT_NAME", StringUtil.getUpperUnderscoreCaseFromLowerCamelCase("userParentName"));
	}

	@Test
	public void testNameToMethodNameWithComma() {
		assertEquals( "entity_Graph", StringUtil.nameToMethodName( "entity,Graph" ) );
	}
}
