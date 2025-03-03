/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.oracle;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.spatial.HibernateSpatialConfigurationSettings;
import org.hibernate.spatial.integration.functions.CommonFunctionTests;
import org.hibernate.spatial.testing.dialects.oracle.OracleSTNativeSqlTemplates;

import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;

/**
 * Only for Oracle: run the tests in "OGC_STRICT" mode (i.e. using the SQL MultiMedia functions)
 */
@RequiresDialect(value = OracleDialect.class)
@ServiceRegistry(settings = {
		@Setting(name = HibernateSpatialConfigurationSettings.ORACLE_OGC_STRICT, value = "true")
})
@SkipForDialect(dialectClass = OracleDialect.class, majorVersion = 11, reason = "See https://hibernate.atlassian.net/browse/HHH-15669")
public class OracleSQLMMFunctionTests extends CommonFunctionTests {
	public OracleSQLMMFunctionTests() {
		this.templates = new OracleSTNativeSqlTemplates();
	}
}
