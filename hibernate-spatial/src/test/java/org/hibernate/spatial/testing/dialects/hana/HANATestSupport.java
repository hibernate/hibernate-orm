/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
