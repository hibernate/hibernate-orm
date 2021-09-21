/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.mappedsuperclass.typedmappedsuperclass;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "METAGEN-37")
public class TypesMappedSuperclassTest extends CompilationTest {
	@Test
	@WithClasses({
			AttachmentGroup.class,
			AttachmentGroupInTopic.class,
			AttachmentGroupPost.class,
			AttachmentGroupPostInTopic.class,
			Post.class,
			UserRole.class
	})
	public void testExtractClosestRealType() {
		assertMetamodelClassGeneratedFor( AttachmentGroup.class );
	}
}
