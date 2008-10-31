/*
 * JBoss, the OpenSource EJB server
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.hibernate.ejb.event;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @version $Revision$
 */
public class BeanCallback extends Callback {
	public BeanCallback(Method callbackMethod) {
		super( callbackMethod );
	}

	public void invoke(Object bean) {
		try {
			callbackMethod.invoke( bean, new Object[0] );
		}
		catch (InvocationTargetException e) {
			//keep runtime exceptions as is
			if ( e.getTargetException() instanceof RuntimeException ) {
				throw (RuntimeException) e.getTargetException();
			}
			else {
				throw new RuntimeException( e.getTargetException() );
			}
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}


}
