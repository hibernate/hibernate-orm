/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.jakarta.data.tck.runner.util;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.transaction.Status;
import jakarta.transaction.Transactional;
import jakarta.transaction.UserTransaction;

@Interceptor
@Transactional
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
public class TransactionalInterceptor {

	@Inject
	UserTransaction ut;

	@AroundInvoke
	public Object intercept(InvocationContext ctx) throws Exception {
		if ( ut.getStatus() == Status.STATUS_ACTIVE ) {
			return ctx.proceed();
		}
		ut.begin();
		try {
			Object result = ctx.proceed();
			ut.commit();
			return result;
		}
		catch (Exception e) {
			if ( ut.getStatus() == Status.STATUS_ACTIVE
				|| ut.getStatus() == Status.STATUS_MARKED_ROLLBACK ) {
				ut.rollback();
			}
			throw e;
		}
	}
}
