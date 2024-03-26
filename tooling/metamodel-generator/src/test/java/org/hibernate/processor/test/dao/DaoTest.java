/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.dao;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Gavin King
 */
public class DaoTest extends CompilationTest {
	@Test
	@WithClasses({ Book.class, Dao.class, Bean.class, StatefulDao.class, StatelessDao.class })
	public void testDao() {
		System.out.println( TestUtil.getMetaModelSourceAsString( Dao.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( StatefulDao.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( StatelessDao.class ) );
		assertMetamodelClassGeneratedFor( Book.class );
		assertMetamodelClassGeneratedFor( Dao.class );
		assertMetamodelClassGeneratedFor( StatefulDao.class );
		assertMetamodelClassGeneratedFor( StatelessDao.class );
	}
}
