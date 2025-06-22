/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.orm.junit.DialectFeatureCheck;

/**
 * Checks if the Dialect is actually supported by Spatial
 * <p>
 * Note: the tests in this module need to be explicitly  enabled in the gradle build config. So this check is
 * maybe no longer needed.
 */
public class IsSupportedBySpatial implements DialectFeatureCheck {
	@Override
	public boolean apply(Dialect dialect) {
		return dialect instanceof PostgreSQLDialect
				|| dialect instanceof CockroachDialect
				|| dialect instanceof MySQLDialect
				|| dialect instanceof MariaDBDialect
				|| dialect instanceof OracleDialect
				|| dialect instanceof SQLServerDialect
				|| dialect instanceof H2Dialect;
	}
}
