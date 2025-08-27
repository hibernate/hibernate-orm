/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.community.dialect.pagination.RowsLimitHandler;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.orm.test.dialect.AbstractLimitHandlerTest;

/**
 * @author Yanming Zhou
 */
public class RowsLimitHandlerTest extends AbstractLimitHandlerTest {

	@Override
	protected AbstractLimitHandler getLimitHandler() {
		return RowsLimitHandler.INSTANCE;
	}

	@Override
	protected String getLimitClause() {
		return " rows ?";
	}
}
