/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.jdbc;

import java.sql.Types;

import org.hibernate.type.descriptor.jdbc.JdbcTypeJavaClassMappings;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Tiger Wang
 */
public class JdbcTypeJavaClassMappingsTest {

	@Before
	public void before() throws Exception {
	}

	@After
	public void after() throws Exception {
	}

	@Test
	public void testDetermineJdbcTypeCodeForJavaClass() throws Exception {
		int jdbcTypeCode = JdbcTypeJavaClassMappings.INSTANCE.determineJdbcTypeCodeForJavaClass( Short.class );
		assertEquals( jdbcTypeCode, Types.SMALLINT );
	}

	@Test
	public void testDetermineJavaClassForJdbcTypeCodeTypeCode() throws Exception {
		Class javaClass = JdbcTypeJavaClassMappings.INSTANCE.determineJavaClassForJdbcTypeCode( Types.SMALLINT );
		assertEquals( javaClass, Short.class );
	}

}
