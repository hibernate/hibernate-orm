/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.embeddable.genericsinheritance;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertAttributeTypeInMetaModelFor;
import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertSuperclassRelationshipInMetamodel;

@CompilationTest
class EmbeddableGenericsAndInterfaceTest {
	@Test @WithClasses({ExampleEntity.class, UserEntity.class, ExampleEmbedded.class, ExampleSuperClassEmbedded.class})
	void test() {
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
