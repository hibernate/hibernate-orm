/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.data.eg;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;

/**
 * @author Gavin King
 */
public class EgTest extends CompilationTest {
	@Test
	@WithClasses({ Publisher.class, Author.class, Address.class, Book.class, Library.class, Bookshop.class, Publishers.class })
	public void test() {
		System.out.println( getMetaModelSourceAsString( Author.class ) );
		System.out.println( getMetaModelSourceAsString( Book.class ) );
		System.out.println( getMetaModelSourceAsString( Author.class, true ) );
		System.out.println( getMetaModelSourceAsString( Book.class, true ) );
		System.out.println( getMetaModelSourceAsString( Library.class ) );
		System.out.println( getMetaModelSourceAsString( Bookshop.class ) );
		System.out.println( getMetaModelSourceAsString( Publishers.class ) );
		assertMetamodelClassGeneratedFor( Author.class, true );
		assertMetamodelClassGeneratedFor( Book.class, true );
		assertMetamodelClassGeneratedFor( Publisher.class, true );
		assertMetamodelClassGeneratedFor( Author.class );
		assertMetamodelClassGeneratedFor( Book.class );
		assertMetamodelClassGeneratedFor( Publisher.class );
		assertMetamodelClassGeneratedFor( Library.class );
		assertMetamodelClassGeneratedFor( Bookshop.class );
		assertMetamodelClassGeneratedFor( Publishers.class );
	}
}
