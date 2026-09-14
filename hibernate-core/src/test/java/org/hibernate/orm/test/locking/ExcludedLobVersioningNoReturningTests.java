/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

import static org.hibernate.cfg.JdbcSettings.DIALECT;

@RequiresDialect(H2Dialect.class)
@ServiceRegistry(settings = @Setting(name = DIALECT,
		value = "org.hibernate.orm.test.locking.ExcludedFromVersioningNoReturningTests$NoReturningDialect"))
public class ExcludedLobVersioningNoReturningTests extends ExcludedLobVersioningTests {
}
