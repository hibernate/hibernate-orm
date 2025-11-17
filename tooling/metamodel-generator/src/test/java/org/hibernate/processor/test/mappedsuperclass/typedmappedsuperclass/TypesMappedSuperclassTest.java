/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.mappedsuperclass.typedmappedsuperclass;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Hardy Ferentschik
 */
@CompilationTest
@TestForIssue(jiraKey = "METAGEN-37")
class TypesMappedSuperclassTest {
	@Test
	@WithClasses({
			AttachmentGroup.class,
			AttachmentGroupInTopic.class,
			AttachmentGroupPost.class,
			AttachmentGroupPostInTopic.class,
			Post.class,
			UserRole.class
	})
	void testExtractClosestRealType() {
		assertMetamodelClassGeneratedFor( AttachmentGroup.class );
	}
}
