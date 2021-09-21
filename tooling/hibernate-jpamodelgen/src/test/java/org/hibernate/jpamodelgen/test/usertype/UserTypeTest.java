/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.usertype;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;

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
