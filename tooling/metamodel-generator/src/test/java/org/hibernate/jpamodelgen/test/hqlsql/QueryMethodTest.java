/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.hqlsql;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestUtil;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Gavin King
 */
public class QueryMethodTest extends CompilationTest {
	@Test
	@WithClasses({ Book.class, Dao.class })
	public void testGeneratedAnnotationNotGenerated() {
		System.out.println( TestUtil.getMetaModelSourceAsString( Dao.class ) );
		assertMetamodelClassGeneratedFor( Book.class );
		assertMetamodelClassGeneratedFor( Dao.class );
	}
}
