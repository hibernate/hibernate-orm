/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bulkid;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.testing.orm.junit.SkipForDialect;

/**
 * Special test that tries to update 1100 rows. Oracle only supports up to 1000 parameters per in-predicate,
 * so we want to test if this scenario works.
 *
 * @author Vlad Mihalcea
 */
@SkipForDialect(
		dialectClass = CockroachDialect.class,
		reason = "Amount of rows lengthens the transaction time, leading to retry errors on CockroachDB: https://www.cockroachlabs.com/docs/v24.3/transaction-retry-error-reference.html#retry_commit_deadline_exceeded"
)
public class OracleInlineMutationStrategyIdTest extends InlineMutationStrategyIdTest {

	@Override
	protected int entityCount() {
		return 1100;
	}
}
