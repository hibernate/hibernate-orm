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
 * {@link org.hibernate.type.StandardBasicTypes#MATERIALIZED_CLOB}.
 *
 * @author Gail Badner
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsExpectedLobUsagePattern.class)
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/lob/MaterializedClobMappings.hbm.xml"
)
public class MaterializedClobTest extends LongStringTest {
}
