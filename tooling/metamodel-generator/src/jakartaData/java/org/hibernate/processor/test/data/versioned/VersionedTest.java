/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.data.versioned;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;

/**
 * @author Gavin King
 */
public class VersionedTest extends CompilationTest {
	@Test
	@WithClasses({ Versioned.class, VersionedRepo.class, SpecialVersioned.class, SpecialVersionedRepo.class })
	public void test() {
		System.out.println( getMetaModelSourceAsString( VersionedRepo.class ) );
		assertMetamodelClassGeneratedFor( Versioned.class, true );
		assertMetamodelClassGeneratedFor( Versioned.class );
		assertMetamodelClassGeneratedFor( SpecialVersioned.class, true );
		assertMetamodelClassGeneratedFor( SpecialVersioned.class );
		assertMetamodelClassGeneratedFor( VersionedRepo.class );
		assertMetamodelClassGeneratedFor( SpecialVersionedRepo.class );
	}
}
