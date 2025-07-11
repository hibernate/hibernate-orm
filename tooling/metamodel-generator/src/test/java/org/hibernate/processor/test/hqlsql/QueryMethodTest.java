/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.hqlsql;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Gavin King
 */
@CompilationTest
class QueryMethodTest {
	@Test
	@WithClasses({ Book.class, Publisher.class, Dao.class, Books.class })
	void testQueryMethod() {
		System.out.println( TestUtil.getMetaModelSourceAsString( Dao.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( Books.class ) );
		assertMetamodelClassGeneratedFor( Book.class );
		assertMetamodelClassGeneratedFor( Dao.class );
		assertMetamodelClassGeneratedFor( Books.class );
	}
}
