/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.lob;


import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;

/**
 * Tests eager materialization and mutation of data mapped by
 * {@link org.hibernate.type.StandardBasicTypes#MATERIALIZED_BLOB}.
 *
 * @author Gail Badner
 */
@RequiresDialectFeature(
		feature = DialectFeatureChecks.SupportsExpectedLobUsagePattern.class,
		comment = "database/driver does not support expected LOB usage pattern"
)
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/lob/MaterializedBlobMappings.hbm.xml"
)
public class MaterializedBlobTest extends LongByteArrayTest {
}
