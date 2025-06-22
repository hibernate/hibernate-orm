/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interfaceproxy;

import java.util.Calendar;

import org.hibernate.CallbackException;
import org.hibernate.Interceptor;
import org.hibernate.type.Type;

/**
 * @author Gavin King
 */
public class DocumentInterceptor implements Interceptor {

	public boolean onFlushDirty(
			Object entity,
			Object id,
			Object[] currentState,
			Object[] previousState,
			String[] propertyNames,
			Type[] types) throws CallbackException {
		if ( entity instanceof Document ) {
			currentState[2] = Calendar.getInstance();
			return true;
		}
		else {
			return false;
		}
	}

	public boolean onPersist(
			Object entity,
			Object id,
			Object[] state,
			String[] propertyNames,
			Type[] types) throws CallbackException {
		if ( entity instanceof Document ) {
			state[3] = state[2] = Calendar.getInstance();
			return true;
		}
		else {
			return false;
		}
	}

	public void onDelete(
			Object entity,
			Object id,
			Object[] state,
			String[] propertyNames,
			Type[] types) throws CallbackException {

	}
}
