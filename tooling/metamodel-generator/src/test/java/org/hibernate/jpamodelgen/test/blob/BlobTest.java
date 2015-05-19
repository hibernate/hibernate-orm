/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.blob;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;

/**
 * @author Hardy Ferentschik
 */
public class BlobTest extends CompilationTest {

	@Test
	@TestForIssue(jiraKey = "METAGEN-38")
	@WithClasses(BlobEntity.class)
	public void testBlobField() {
		assertMetamodelClassGeneratedFor( BlobEntity.class );
		assertPresenceOfFieldInMetamodelFor( BlobEntity.class, "blob", "the metamodel should have a member 'blob'" );
	}
}
