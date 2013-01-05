//$Id: PropertyInterceptor.java 7700 2005-07-30 05:02:47Z oneovthafew $
package org.hibernate.test.interceptor;
import java.io.Serializable;
import java.util.Calendar;

import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;

public class PropertyInterceptor extends EmptyInterceptor {

	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
		for(int i=0;i<propertyNames.length;i++){
			if("lastUpdated".equals( propertyNames[i] )){
				currentState[i]=Calendar.getInstance();
				break;
			}
		}
		return true;
	}

	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		for(int i=0;i<propertyNames.length;i++){
			if("created".equals( propertyNames[i] )){
				state[i]=Calendar.getInstance();
				break;
			}
		}
		return true;
	}

}
