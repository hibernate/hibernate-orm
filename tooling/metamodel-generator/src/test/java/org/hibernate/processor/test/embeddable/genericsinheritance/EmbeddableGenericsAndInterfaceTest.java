/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.embeddable.genericsinheritance;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertAttributeTypeInMetaModelFor;
import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertSuperclassRelationshipInMetamodel;

public class EmbeddableGenericsAndInterfaceTest extends CompilationTest {
	@Test @WithClasses({ExampleEntity.class, UserEntity.class, ExampleEmbedded.class, ExampleSuperClassEmbedded.class})
	public void test() {
		System.out.println( TestUtil.getMetaModelSourceAsString( ExampleEntity.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( ExampleSuperClassEmbedded.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( ExampleEmbedded.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( UserEntity.class ) );
		assertMetamodelClassGeneratedFor( ExampleEntity.class );
		assertMetamodelClassGeneratedFor( ExampleSuperClassEmbedded.class );
		assertMetamodelClassGeneratedFor( ExampleEmbedded.class );
		assertMetamodelClassGeneratedFor( UserEntity.class );
		assertSuperclassRelationshipInMetamodel( ExampleEmbedded.class, ExampleSuperClassEmbedded.class );
		assertAttributeTypeInMetaModelFor(
				ExampleSuperClassEmbedded.class,
				"user",
				UserEntity.class,
				"user should be inherited"
		);
	}
}
