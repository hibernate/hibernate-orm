/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.descriptor.sql;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Clob;
import java.sql.ResultSet;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class ResultSetProxy<T> implements InvocationHandler {
	public static ResultSet generateProxy(ResultSetProxy handler) {
		return ( ResultSet ) Proxy.newProxyInstance(
				getProxyClassLoader(),
				new Class[] { ResultSet.class },
				handler
		);
	}

	private static ClassLoader getProxyClassLoader() {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if ( cl == null ) {
			cl = ResultSet.class.getClassLoader();
		}
		return cl;
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if ( method.getName().equals( methodName ) && args.length >= 1 ) {
			return value;
		}
		if ( method.getName().equals( "wasNull" ) ) {
			return value == null;
		}
		throw new UnsupportedOperationException( "Unexpected call ResultSet." + method.getName() );
	}

	private final String methodName;
	private final T value;

	protected ResultSetProxy(String methodName, T value) {
		this.methodName = methodName;
		this.value = value;
	}

	public static ResultSet generateProxy(final String value) {
		return generateProxy(
				new ResultSetProxy<String>( "getString", value )
		);
	}

	public static ResultSet generateProxy(final Clob value) {
		return generateProxy(
				new ResultSetProxy<Clob>( "getClob", value )
		);
	}
}
