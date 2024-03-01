/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.generatedannotation;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class GeneratedAnnotationTest extends CompilationTest {

	@Test
	@TestForIssue(jiraKey = "METAGEN-79")
	@WithClasses(TestEntity.class)
	public void testGeneratedAnnotationNotGenerated() {
		assertMetamodelClassGeneratedFor( TestEntity.class );

		// need to check the source because @Generated is not a runtime annotation
		String metaModelSource = getMetaModelSourceAsString( TestEntity.class );
		String generatedString = "@Generated(\"org.hibernate.processor.HibernateProcessor\")";
		assertTrue( "@Generated should be added to the metamodel.", metaModelSource.contains( generatedString ) );
	}
}
