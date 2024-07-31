/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.mappedsuperclass.typedmappedsuperclass;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Hardy Ferentschik
 */
@JiraKey(value = "METAGEN-37")
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
