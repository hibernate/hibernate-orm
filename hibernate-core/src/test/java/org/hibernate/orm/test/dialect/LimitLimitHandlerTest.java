/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitLimitHandler;

/**
 * @author Yanming Zhou
 */
public class LimitLimitHandlerTest extends AbstractLimitHandlerTest {

	@Override
	protected AbstractLimitHandler getLimitHandler() {
		return LimitLimitHandler.INSTANCE;
	}
}
