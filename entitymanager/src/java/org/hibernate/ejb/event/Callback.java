/*
 * JBoss, the OpenSource EJB server
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.hibernate.ejb.event;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.hibernate.util.ReflectHelper;

/**
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @version $Revision$
 */
public abstract class Callback implements Serializable {
	transient protected Method callbackMethod;

	public Callback(Method callbackMethod) {
		this.callbackMethod = callbackMethod;
	}

	public Method getCallbackMethod() {
		return callbackMethod;
	}

	public abstract void invoke(Object bean);

	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();
		oos.writeObject( callbackMethod.toGenericString() );
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		String signature = (String) ois.readObject();
		StringTokenizer st = new StringTokenizer( signature, " ", false );
		String usefulSignature = null;
		while ( st.hasMoreElements() ) usefulSignature = (String) st.nextElement();
		int parenthesis = usefulSignature.indexOf( "(" );
		String methodAndClass = usefulSignature.substring( 0, parenthesis );
		int lastDot = methodAndClass.lastIndexOf( "." );
		String clazzName = methodAndClass.substring( 0, lastDot );
		Class callbackClass = ReflectHelper.classForName( clazzName, this.getClass() );
		String parametersString = usefulSignature.substring( parenthesis + 1, usefulSignature.length() - 1 );
		st = new StringTokenizer( parametersString, ", ", false );
		List<Class> parameters = new ArrayList<Class>();
		while ( st.hasMoreElements() ) {
			String parameter = (String) st.nextElement();
			parameters.add( ReflectHelper.classForName( parameter, this.getClass() ) );
		}
		String methodName = methodAndClass.substring( lastDot + 1, methodAndClass.length() );
		try {
			callbackMethod = callbackClass.getDeclaredMethod(
					methodName,
					parameters.toArray( new Class[ parameters.size() ] )
			);
			if ( ! callbackMethod.isAccessible() ) {
				callbackMethod.setAccessible( true );
			}
		}
		catch (NoSuchMethodException e) {
			throw new IOException( "Unable to get EJB3 callback method: " + signature + ", cause: " + e );
		}
	}
}
