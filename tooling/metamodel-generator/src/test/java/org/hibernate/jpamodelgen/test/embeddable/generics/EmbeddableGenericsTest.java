/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.embeddable.generics;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.TestUtil;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertSetAttributeTypeInMetaModelFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertSuperClassRelationShipInMetamodel;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH_12030")
public class EmbeddableGenericsTest extends CompilationTest {
	@Test
	@WithClasses({ ChildEmbeddable.class, ParentEmbeddable.class })
	public void testGeneratingEmbeddablesWithGenerics() {
		assertMetamodelClassGeneratedFor( ChildEmbeddable.class );
		assertMetamodelClassGeneratedFor( ParentEmbeddable.class );

		assertSetAttributeTypeInMetaModelFor(
				ParentEmbeddable.class,
				"fields",
				MyTypeInterface.class,
				"Expected Set<MyTypeInterface> for attribute named 'fields'"
		);

		assertSuperClassRelationShipInMetamodel(
				ChildEmbeddable.class,
				ParentEmbeddable.class
		);
	}
}
