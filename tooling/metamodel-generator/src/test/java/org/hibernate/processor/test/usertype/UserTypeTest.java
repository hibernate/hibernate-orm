/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.usertype;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "METAGEN-28")
public class UserTypeTest extends CompilationTest {
	@Test
	@WithClasses({ ContactDetails.class, PhoneNumber.class })
	public void testCustomUserTypeInMetaModel() {
		assertMetamodelClassGeneratedFor( ContactDetails.class );
		assertPresenceOfFieldInMetamodelFor(
				ContactDetails.class, "phoneNumber", "@Type annotated field should be in metamodel"
		);
	}
}
