/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interceptor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.type.Type;

public class StatefulInterceptor implements Interceptor {

	private Session session;

	private List list = new ArrayList();

	@Override
	public boolean onPersist(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
		if ( !(entity instanceof Log) ) {
			list.add( new Log( "insert", (String) id, entity.getClass().getName() ) );
		}
		return false;
	}

	@Override
	public boolean onFlushDirty(Object entity, Object id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
		if ( !(entity instanceof Log) ) {
			list.add( new Log( "update", (String) id, entity.getClass().getName() ) );
		}
		return false;
	}

	@Override
	public void postFlush(Iterator entities) {
		if ( list.size()>0 ) {
			for ( Iterator iter = list.iterator(); iter.hasNext(); ) {
				session.persist( iter.next() );
			}
			list.clear();
			session.flush();
		}
	}

	public void setSession(Session s) {
		session = s;
	}

}
