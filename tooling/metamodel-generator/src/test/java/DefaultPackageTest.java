/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Hardy Ferentschik
 */
@CompilationTest
@TestForIssue(jiraKey = "METAGEN-40")
@WithClasses(DefaultPackageEntity.class)
class DefaultPackageTest {
	@Test
	void testMetaModelGeneratedForEntitiesInDefaultPackage() {
		assertMetamodelClassGeneratedFor( DefaultPackageEntity.class );
	}
}
