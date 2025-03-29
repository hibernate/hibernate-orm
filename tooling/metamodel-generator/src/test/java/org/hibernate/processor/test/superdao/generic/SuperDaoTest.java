/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.superdao.generic;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Gavin King
 */
public class SuperDaoTest extends CompilationTest {
	@Test
	@WithClasses({ Book.class, SuperDao.class, Dao.class })
	public void testQueryMethod() {
		System.out.println( TestUtil.getMetaModelSourceAsString( SuperDao.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( Dao.class ) );
		assertMetamodelClassGeneratedFor( Book.class );
		assertMetamodelClassGeneratedFor( SuperDao.class );
		assertMetamodelClassGeneratedFor( Dao.class );
	}
}
