/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.separatecompilationunits;

import org.hibernate.jpamodelgen.test.separatecompilationunits.superclass.MappedSuperclass;
import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.IgnoreCompilationErrors;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "METAGEN-35")
public class SeparateCompilationUnitsTest extends CompilationTest {
	@Test
	@WithClasses(value = Entity.class, preCompile = MappedSuperclass.class)
	@IgnoreCompilationErrors
	public void testInheritance() throws Exception {
		// need to work with the source file. Entity_.class won't get generated, because the mapped superclass
		// will not be on the classpath
		String entityMetaModel = getMetaModelSourceAsString( Entity.class );
		assertTrue(
				entityMetaModel.contains(
						"extends org.hibernate.jpamodelgen.test.separatecompilationunits.superclass.MappedSuperclass"
				)
		);
	}
}
