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
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import org.hibernate.util.ReflectHelper;

/**
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 */
public class ListenerCallback extends Callback {
	protected transient Object listener;

	public ListenerCallback(Method callbackMethod, Object listener) {
		super( callbackMethod );
		this.listener = listener;
	}

	public void invoke(Object bean) {
		try {
			callbackMethod.invoke( listener, new Object[]{bean} );
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

	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();
		oos.writeObject( listener.getClass().getName() );
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		String listenerClass = (String) ois.readObject();
		try {
			listener = ReflectHelper.classForName( listenerClass, this.getClass() ).newInstance();
		}
		catch (InstantiationException e) {
			throw new ClassNotFoundException( "Unable to load class:" + listenerClass, e );
		}
		catch (IllegalAccessException e) {
			throw new ClassNotFoundException( "Unable to load class:" + listenerClass, e );
		}
	}
}
