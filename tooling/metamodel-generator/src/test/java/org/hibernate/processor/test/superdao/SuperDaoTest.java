/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.superdao;

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
//		System.out.println( TestUtil.getMetaModelSourceAsString( SuperDao.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( Dao.class ) );
		assertMetamodelClassGeneratedFor( Book.class );
//		assertMetamodelClassGeneratedFor( SuperDao.class );
		assertMetamodelClassGeneratedFor( Dao.class );
	}
}
