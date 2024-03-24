/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.data.superdao.generic;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Gavin King
 */
public class SuperRepoTest extends CompilationTest {
	@Test
	@WithClasses({ Book.class, SuperRepo.class, Repo.class })
	public void testQueryMethod() {
//		System.out.println( TestUtil.getMetaModelSourceAsString( SuperRepo.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( Repo.class ) );
		assertMetamodelClassGeneratedFor( Book.class );
//		assertMetamodelClassGeneratedFor( SuperRepo.class );
		assertMetamodelClassGeneratedFor( Repo.class );
	}
}
