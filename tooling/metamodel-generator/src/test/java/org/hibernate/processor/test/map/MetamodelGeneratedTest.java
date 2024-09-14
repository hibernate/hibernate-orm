/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.map;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.hibernate.processor.test.util.TestUtil.getMetamodelClassFor;

public class MetamodelGeneratedTest extends CompilationTest {

	@Test
	@WithClasses({ MapOfMapEntity.class })
	@TestForIssue(jiraKey = " HHH-17514")
	public void test() {
		Class<?> repositoryClass = getMetamodelClassFor( MapOfMapEntity.class );
		Assertions.assertNotNull( repositoryClass );
	}
}
