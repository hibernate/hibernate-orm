/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.descriptor.sql;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class PreparedStatementProxy<T> implements InvocationHandler {
	public static PreparedStatement generateProxy(PreparedStatementProxy handler) {
		return (PreparedStatement) Proxy.newProxyInstance(
				getProxyClassLoader(),
				new Class[] { PreparedStatement.class },
				handler
		);
	}

	private static ClassLoader getProxyClassLoader() {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if ( cl == null ) {
			cl = PreparedStatement.class.getClassLoader();
		}
		return cl;
	}

	@SuppressWarnings("unchecked")
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if ( value == null ) {
			assertEquals( "setNull", method.getName(), "Expecting setNull call" );
			return null;
		}
		if ( method.getName().equals( methodName ) && args.length >= 1 ) {
			checkValue( (T) args[1] );
			return null;
		}
		throw new UnsupportedOperationException( "Unexpected call PreparedStatement." + method.getName() );
	}

	protected void checkValue(T arg) throws SQLException {
		assertEquals( value, arg );
	}

	protected final String extractString(Clob clob) throws SQLException {
		if ( StringClobImpl.class.isInstance( clob ) ) {
			return ( (StringClobImpl) clob ).getValue();
		}
		return clob.getSubString( 1, (int) clob.length() );
	}

	private final String methodName;
	private final T value;

	public T getValue() {
		return value;
	}

	protected PreparedStatementProxy(String methodName, T value) {
		this.methodName = methodName;
		this.value = value;
	}

	public static PreparedStatement generateProxy(final String value) {
		return generateProxy(
				new PreparedStatementProxy<String>( "setString", value )
		);
	}

	public static PreparedStatement generateProxy(Clob value) {
		return generateProxy(
				new PreparedStatementProxy<Clob>( "setClob", value ) {
					@Override
					protected void checkValue(Clob arg) throws SQLException {
						assertEquals( extractString( getValue() ), extractString( arg ) );
					}
				}
		);
	}
}
