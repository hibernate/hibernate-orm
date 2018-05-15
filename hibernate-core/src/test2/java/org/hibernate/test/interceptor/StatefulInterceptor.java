/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: StatefulInterceptor.java 7701 2005-07-30 05:07:01Z oneovthafew $
package org.hibernate.test.interceptor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Session;
import org.hibernate.type.Type;

public class StatefulInterceptor extends EmptyInterceptor {
	
	private Session session;

	private List list = new ArrayList();
	
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		if ( !(entity instanceof Log) ) {
			list.add( new Log( "insert", (String) id, entity.getClass().getName() ) );
		}
		return false;
	}

	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
		if ( !(entity instanceof Log) ) {
			list.add( new Log( "update", (String) id, entity.getClass().getName() ) );
		}
		return false;
	}

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
