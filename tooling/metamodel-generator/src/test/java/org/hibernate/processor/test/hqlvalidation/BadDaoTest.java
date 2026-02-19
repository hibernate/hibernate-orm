/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.hqlvalidation;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.IgnoreCompilationErrors;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertNoMetamodelClassGeneratedFor;

/**
 * @author Gavin King
 */
@CompilationTest
class BadDaoTest {
	@Test
	@WithClasses({ Book.class, Author.class, BadDao.class })
	@IgnoreCompilationErrors
	void testDao() {
		System.out.println( TestUtil.getMetaModelSourceAsString( BadDao.class ) );
//		assertMetamodelClassGeneratedFor( Book.class );
//		assertMetamodelClassGeneratedFor( Author.class );
		assertNoMetamodelClassGeneratedFor( BadDao.class );
	}
}
