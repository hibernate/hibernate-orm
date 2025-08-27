/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
