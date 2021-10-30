/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.orm.junit.DialectFeatureCheck;

/**
 * Checks if the Dialect is actually supported by Spatial
 *
 * Note: the tests in this module need to be explicitly  enabled in the gradle build config. So this check is
 * maybe no longer needed.
 *
 */
public class IsSupportedBySpatial implements DialectFeatureCheck {
	@Override
	public boolean apply(Dialect dialect) {
		return dialect instanceof PostgreSQLDialect
				|| dialect instanceof CockroachDialect
				|| dialect instanceof MariaDBDialect;
	}
}
