/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.data.data;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.getMetaModelSourceAsString;

/**
 * @author Gavin King
 */
public class DataTest extends CompilationTest {
	@Test
	@WithClasses({ Author.class, Book.class, BookAuthorRepository.class })
	public void test() {
		System.out.println( getMetaModelSourceAsString( Author.class ) );
		System.out.println( getMetaModelSourceAsString( Book.class ) );
		System.out.println( getMetaModelSourceAsString( Author.class, true ) );
		System.out.println( getMetaModelSourceAsString( Book.class, true ) );
		System.out.println( getMetaModelSourceAsString( BookAuthorRepository.class ) );
		assertMetamodelClassGeneratedFor( Author.class, true );
		assertMetamodelClassGeneratedFor( Book.class, true );
		assertMetamodelClassGeneratedFor( Author.class );
		assertMetamodelClassGeneratedFor( Book.class );
		assertMetamodelClassGeneratedFor( BookAuthorRepository.class );
	}
}
