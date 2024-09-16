/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "METAGEN-40")
@WithClasses(DefaultPackageEntity.class)
public class DefaultPackageTest extends CompilationTest {
	@Test
	public void testMetaModelGeneratedForEntitiesInDefaultPackage() {
		assertMetamodelClassGeneratedFor( DefaultPackageEntity.class );
	}
}
