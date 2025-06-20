/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.mappedsuperclass.dao;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Gavin King
 */
@CompilationTest
class DaoTest {
	@Test
	@WithClasses({ Parent.class, Child.class, Queries.class })
	void testDao() {
		System.out.println( TestUtil.getMetaModelSourceAsString( Queries.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( Parent.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( Child.class ) );
		assertMetamodelClassGeneratedFor( Queries.class );
		assertMetamodelClassGeneratedFor( Parent.class );
		assertMetamodelClassGeneratedFor( Child.class );
	}
}
