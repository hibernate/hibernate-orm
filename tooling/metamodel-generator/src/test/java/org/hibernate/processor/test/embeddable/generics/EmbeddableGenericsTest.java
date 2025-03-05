/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.embeddable.generics;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static java.lang.System.out;
import static org.hibernate.processor.test.util.TestUtil.*;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH_12030")
public class EmbeddableGenericsTest extends CompilationTest {
	@Test
	@WithClasses({ ChildEmbeddable.class, ParentEmbeddable.class })
	public void testGeneratingEmbeddablesWithGenerics() {
		out.println( getMetaModelSourceAsString(ParentEmbeddable.class) );
		assertMetamodelClassGeneratedFor( ChildEmbeddable.class );
		assertMetamodelClassGeneratedFor( ParentEmbeddable.class );

//		assertAttributeTypeInMetaModelFor(
//				ParentEmbeddable.class,
//				"fields",
//				"java.util.Set<? extends MyTypeInterface>",
//				"Expected Set for attribute named 'fields'"
//		);

		assertSuperclassRelationshipInMetamodel(
				ChildEmbeddable.class,
				ParentEmbeddable.class
		);
	}
}
