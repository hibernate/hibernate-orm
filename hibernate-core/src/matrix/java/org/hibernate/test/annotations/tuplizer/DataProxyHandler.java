//$Id$
package org.hibernate.test.annotations.tuplizer;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * A simple {@link java.lang.reflect.InvocationHandler} to act as the handler for our generated
 * {@link java.lang.reflect.Proxy}-based entity instances.
 * <p/>
 * This is a trivial impl which simply keeps the property values into
 * a Map.
 *
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
 */
public final class DataProxyHandler implements InvocationHandler {
	private String entityName;
	private HashMap data = new HashMap();

	public DataProxyHandler(String entityName, Serializable id) {
		this.entityName = entityName;
		data.put( "Id", id );
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		String methodName = method.getName();
		if ( methodName.startsWith( "set" ) ) {
			String propertyName = methodName.substring( 3 );
			data.put( propertyName, args[0] );
		}
		else if ( methodName.startsWith( "get" ) ) {
			String propertyName = methodName.substring( 3 );
			return data.get( propertyName );
		}
		else if ( "toString".equals( methodName ) ) {
			return entityName + "#" + data.get( "Id" );
		}
		else if ( "hashCode".equals( methodName ) ) {
			return new Integer( this.hashCode() );
		}
		return null;
	}

	public String getEntityName() {
		return entityName;
	}

	public HashMap getData() {
		return data;
	}
}
