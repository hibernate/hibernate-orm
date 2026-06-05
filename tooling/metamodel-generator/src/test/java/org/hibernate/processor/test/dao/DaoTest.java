/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.dao;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@CompilationTest
class DaoTest {
	@Test
	@WithClasses({ Book.class, Dao.class, Bean.class, StatefulDao.class, StatelessDao.class })
	void testDao() {
		final String dao = getMetaModelSourceAsString( Dao.class, true );
		final String statefulDao = getMetaModelSourceAsString( StatefulDao.class, true );
		final String statelessDao = getMetaModelSourceAsString( StatelessDao.class, true );
		System.out.println( dao );
		System.out.println( statefulDao );
		System.out.println( statelessDao );
		assertMetamodelClassGeneratedFor( Book.class );
		assertMetamodelClassGeneratedFor( Dao.class, true );
		assertMetamodelClassGeneratedFor( StatefulDao.class, true );
		assertMetamodelClassGeneratedFor( StatelessDao.class, true );
		assertTrue( dao.contains( ".find(Book.class, isbn, new EnabledFetchProfile(\"Goodbye\"))" ) );
		assertTrue( statefulDao.contains( ".find(Book.class, isbn, new EnabledFetchProfile(\"Goodbye\"))" ) );
		assertTrue( statelessDao.contains( ".find(Book.class, isbn, new EnabledFetchProfile(\"Goodbye\"))" ) );
		assertTrue( statelessDao.contains( ".find(Book.class, _key, KeyType.NATURAL, new EnabledFetchProfile(\"Hello\"))" ) );
		assertTrue( dao.contains( "@Nonnull\n\tpublic Book findByIsbn(String isbn)" ) );
		assertTrue( dao.contains( "@Nullable\n\tpublic Long one()" ) );
		assertTrue( statefulDao.contains(
				"@Nonnull\n\tpublic Book getBook(String title, String author)" ) );
		assertTrue( statefulDao.contains(
				"@Nullable\n\tpublic Book getBookOrNull(String title, String author)" ) );
		assertTrue( statefulDao.contains( "if (_result == null) throw new ObjectNotFoundException((Object) _key" ) );
		assertFalse( dao.contains( ".byId(" ) );
		assertFalse( statefulDao.contains( ".byId(" ) );
		assertFalse( statelessDao.contains( ".byId(" ) );
	}
}
