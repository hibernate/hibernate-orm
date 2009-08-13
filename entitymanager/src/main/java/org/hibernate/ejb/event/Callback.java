/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
