/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.check;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;


/**
 * @author Steve Ebersole
 */
@RequiresDialect(value = OracleDialect.class)
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/sql/check/oracle-mappings.hbm.xml",
		overrideCacheStrategy = false
)
public class OracleCheckStyleTest extends ResultCheckStyleTest {
}
