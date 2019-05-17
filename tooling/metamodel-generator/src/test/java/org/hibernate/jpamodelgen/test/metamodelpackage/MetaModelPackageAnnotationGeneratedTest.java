/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.metamodelpackage;

import org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor;
import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.hibernate.jpamodelgen.test.util.WithProcessorOption;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Marvin S. Addison
 */
public class MetaModelPackageAnnotationGeneratedTest extends CompilationTest {

	private static final String ALT_PACKAGE = "vt.edu";

	@Test
	@TestForIssue(jiraKey = "HHH-13408")
	@WithClasses(TestEntity.class)
	@WithProcessorOption(key = JPAMetaModelEntityProcessor.META_MODEL_PACKAGE, value = ALT_PACKAGE)
	public void testMetaModelPackageAnnotationGenerated() {
		assertMetamodelClassGeneratedFor( TestEntity.class, ALT_PACKAGE );
	}
}
