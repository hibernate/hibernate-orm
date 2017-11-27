/*
 *
 *  * Hibernate, Relational Persistence for Idiomatic Java
 *  *
 *  * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 *  * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 *
 */
package org.hibernate.type.descriptor.sql;

import java.sql.Types;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * JdbcTypeJavaClassMappings Tester.
 *
 * @author Tiger Wang
 * @version 1.0
 * @since <pre>Nov 24, 2017</pre>
 */
public class JdbcTypeJavaClassMappingsTest {

	@Before
	public void before() throws Exception {
	}

	@After
	public void after() throws Exception {
	}

	/**
	 * Method: determineJdbcTypeCodeForJavaClass(Class cls)
	 */
	@Test
	public void testDetermineJdbcTypeCodeForJavaClass() throws Exception {
		int jdbcTypeCode = JdbcTypeJavaClassMappings.INSTANCE.determineJdbcTypeCodeForJavaClass( Short.class );
		assertEquals( jdbcTypeCode, Types.SMALLINT );
	}

	/**
	 * Method: determineJavaClassForJdbcTypeCode(Integer typeCode)
	 */
	@Test
	public void testDetermineJavaClassForJdbcTypeCodeTypeCode() throws Exception {
		Class javaClass = JdbcTypeJavaClassMappings.INSTANCE.determineJavaClassForJdbcTypeCode( Types.SMALLINT );
		assertEquals( javaClass, Short.class );
	}

}
