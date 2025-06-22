/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.mappedsuperclass.typedmappedsuperclass;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

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
