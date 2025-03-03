/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.selectenumproperty;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;

public class TopicTypeEnumTest extends CompilationTest {
	@Test
	@WithClasses({Topic.class, TopicRepository.class})
	public void test() {
		System.out.println( getMetaModelSourceAsString( Topic.class ) );
		System.out.println( getMetaModelSourceAsString( Topic.class, true ) );
		System.out.println( getMetaModelSourceAsString( TopicRepository.class ) );
		assertMetamodelClassGeneratedFor( Topic.class, true );
		assertMetamodelClassGeneratedFor( Topic.class );
		assertMetamodelClassGeneratedFor( TopicRepository.class );
	}
}
