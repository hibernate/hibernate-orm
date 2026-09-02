/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.pagination.spi.AbstractLimitHandler;
import org.hibernate.dialect.pagination.spi.LimitLimitHandler;

/**
 * @author Yanming Zhou
 */
public class LimitLimitHandlerTest extends AbstractLimitHandlerTest {

	@Override
	protected AbstractLimitHandler getLimitHandler() {
		return LimitLimitHandler.INSTANCE;
	}
}
