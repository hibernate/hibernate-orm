/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interceptor;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Interceptor;
import org.hibernate.type.Type;

public class StatefulInterceptor implements Interceptor {

	private final List<Log> logs = new ArrayList<>();

	@Override
	public boolean onPersist(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
		if ( !(entity instanceof Log) ) {
			logs.add( new Log( "insert", (String) id, entity.getClass().getName() ) );
		}
		return false;
	}

	@Override
	public boolean onFlushDirty(Object entity, Object id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
		if ( !(entity instanceof Log) ) {
			logs.add( new Log( "update", (String) id, entity.getClass().getName() ) );
		}
		return false;
	}

	public List<Log> drainLogs() {
		final List<Log> collectedLogs = new ArrayList<>( logs );
		logs.clear();
		return collectedLogs;
	}
}
