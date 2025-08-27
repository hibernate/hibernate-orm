/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.DB2LimitHandler;
import org.hibernate.testing.orm.junit.RequiresDialect;

/**
 * @author Yanming Zhou
 */
@RequiresDialect(DB2Dialect.class)
public class DB2LimitHandlerTest extends AbstractLimitHandlerTest {

	@Override
	protected AbstractLimitHandler getLimitHandler() {
		return DB2LimitHandler.INSTANCE;
	}
}
