/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.embeddable;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.TestUtil;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertAttributeTypeInMetaModelFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-12612")
public class EmbeddableTypeUseTest extends CompilationTest {
	@Test
	@WithClasses({SimpleEntity.class})
	public void testAnnotatedEmbeddable() {
		System.out.println( TestUtil.getMetaModelSourceAsString( SimpleEntity.class ) );
		assertMetamodelClassGeneratedFor( SimpleEntity.class );
		assertAttributeTypeInMetaModelFor(
				SimpleEntity.class,
				"simpleEmbeddable",
				SimpleEmbeddable.class,
				"Wrong type for embeddable attribute."
		);
	}
}
