/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.lob;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;

/**
 * Tests eager materialization and mutation of data mapped by
 * {@link org.hibernate.type.MaterializedBlobType}.
 *
 * @author Gail Badner
 */
@RequiresDialectFeature(
		value = DialectChecks.SupportsExpectedLobUsagePattern.class,
		comment = "database/driver does not support expected LOB usage pattern"
)
public class MaterializedBlobTest extends LongByteArrayTest {
	public String[] getMappings() {
		return new String[] { "lob/MaterializedBlobMappings.hbm.xml" };
	}
}
