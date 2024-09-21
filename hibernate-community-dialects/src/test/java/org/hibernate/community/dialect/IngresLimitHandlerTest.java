/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.community.dialect.pagination.IngresLimitHandler;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.orm.test.dialect.AbstractLimitHandlerTest;

/**
 * @author Yanming Zhou
 */
public class IngresLimitHandlerTest extends AbstractLimitHandlerTest {

	@Override
	protected AbstractLimitHandler getLimitHandler() {
		return IngresLimitHandler.INSTANCE;
	}
}
