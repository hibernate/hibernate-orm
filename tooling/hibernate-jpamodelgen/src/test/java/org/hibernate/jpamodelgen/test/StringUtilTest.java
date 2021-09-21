/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test;

import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.util.StringUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
}
