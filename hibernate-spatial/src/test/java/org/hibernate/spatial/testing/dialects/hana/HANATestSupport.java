/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.dialects.hana;

import org.hibernate.spatial.testing.datareader.TestData;
import org.hibernate.spatial.testing.datareader.TestSupport;

public class HANATestSupport extends TestSupport {


	@Override
	public TestData createTestData(TestDataPurpose purpose) {
		return TestData.fromFile( "hana/test-hana-data-set.xml" );
	}

}
