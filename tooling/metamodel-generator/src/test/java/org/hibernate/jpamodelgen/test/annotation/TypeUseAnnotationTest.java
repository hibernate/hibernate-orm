/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.annotation;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.junit.Test;

import java.sql.Blob;

import static java.lang.String.format;
import static org.hibernate.jpamodelgen.test.util.TestUtil.*;

/**
 * @author Valentin Rentschler
 */
public class TypeUseAnnotationTest extends CompilationTest {

	@Test
	@TestForIssue(jiraKey = "HHH-12011")
	@WithClasses(TypeUseAnnotationEntity.class)
	public void testTypeUseAnnotation() {
		assertMetamodelClassGeneratedFor( TypeUseAnnotationEntity.class );
		assertPresenceOfFieldFor( "blob", Blob.class );
		assertPresenceOfFieldFor( "string", String.class );
		assertPresenceOfFieldFor( "id", String.class );
		assertPresenceOfFieldFor( "bytes", byte[].class );
		assertPresenceOfFieldFor( "bytesWoAnnotation", byte[].class );
		assertPresenceOfFieldFor( "bytesAlt", Byte[].class );
		assertPresenceOfFieldFor( "bytesAltWoAnnotation", Byte[].class );
	}

	private void assertPresenceOfFieldFor(String fieldName, Class<?> expectedType) {
		assertPresenceOfFieldInMetamodelFor( TypeUseAnnotationEntity.class, fieldName,
				format("the metamodel should have a member '%s'", fieldName) );
		assertAttributeTypeInMetaModelFor( TypeUseAnnotationEntity.class,
				fieldName, expectedType, format("the metamodel should have a member '%s' of type '%s'", fieldName, expectedType)
		);
	}
}
