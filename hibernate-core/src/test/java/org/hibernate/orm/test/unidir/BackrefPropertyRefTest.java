/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.unidir;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;

/**
 * @author Gail Badner
 */
@JiraKey( value = "HHH-9370")
@FailureExpected( jiraKey = "HHH-9370")
public class BackrefPropertyRefTest extends BackrefTest {

	@Override
	protected String[] getMappings() {
		return new String[] { "unidir/ParentChildPropertyRef.hbm.xml" };
	}

}
