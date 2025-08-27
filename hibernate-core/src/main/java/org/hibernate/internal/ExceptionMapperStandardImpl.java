/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import jakarta.transaction.SystemException;

import org.hibernate.TransactionException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.ExceptionMapper;

/**
 * @author Steve Ebersole
 */
public class ExceptionMapperStandardImpl implements ExceptionMapper {

	public static final ExceptionMapper INSTANCE = new ExceptionMapperStandardImpl();

	@Override
	public RuntimeException mapStatusCheckFailure(
			String message,
			SystemException systemException,
			SessionImplementor sessionImplementor) {
		return new TransactionException(
				"could not determine transaction status in beforeCompletion()",
				systemException
		);
	}

	@Override
	public RuntimeException mapManagedFlushFailure(
			String message,
			RuntimeException failure,
			SessionImplementor session) {
		return failure;
	}
}
