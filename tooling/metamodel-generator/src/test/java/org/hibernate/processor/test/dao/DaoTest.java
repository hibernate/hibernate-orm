/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.dao;

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
	@WithClasses({ Book.class, Dao.class, Bean.class, StatefulDao.class, StatelessDao.class })
	void testDao() {
		System.out.println( TestUtil.getMetaModelSourceAsString( Dao.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( StatefulDao.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( StatelessDao.class ) );
		assertMetamodelClassGeneratedFor( Book.class );
		assertMetamodelClassGeneratedFor( Dao.class );
		assertMetamodelClassGeneratedFor( StatefulDao.class );
		assertMetamodelClassGeneratedFor( StatelessDao.class );
	}
}
