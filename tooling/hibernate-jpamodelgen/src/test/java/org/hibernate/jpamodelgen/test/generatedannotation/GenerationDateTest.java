/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.generatedannotation;

import org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor;
import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.hibernate.jpamodelgen.test.util.WithProcessorOption;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.dumpMetaModelSourceFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class GenerationDateTest extends CompilationTest {
	@Test
	@TestForIssue(jiraKey = "METAGEN-73")
	@WithClasses(TestEntity.class)
	@WithProcessorOption(key = JPAMetaModelEntityProcessor.ADD_GENERATION_DATE, value = "true")
	public void testGeneratedAnnotationGenerated() {
		assertMetamodelClassGeneratedFor( TestEntity.class );

		// need to check the source because @Generated is not a runtime annotation
		String metaModelSource = getMetaModelSourceAsString( TestEntity.class );

		dumpMetaModelSourceFor( TestEntity.class );
		String generatedString = "@Generated(value = \"org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor\", date = \"";

		assertTrue( "@Generated should also contain the date parameter.", metaModelSource.contains( generatedString ) );
	}
}
