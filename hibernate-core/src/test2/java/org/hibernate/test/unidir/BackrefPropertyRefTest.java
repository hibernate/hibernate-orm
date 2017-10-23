/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.unidir;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-9370")
@FailureExpected( jiraKey = "HHH-9370")
public class BackrefPropertyRefTest extends BackrefTest {

	@Override
	protected String[] getMappings() {
		return new String[] { "unidir/ParentChildPropertyRef.hbm.xml" };
	}

}
