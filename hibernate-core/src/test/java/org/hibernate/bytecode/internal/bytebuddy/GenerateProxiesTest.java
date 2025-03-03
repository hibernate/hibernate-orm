/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal.bytebuddy;

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.InvocationTargetException;

import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyHelper;
import org.junit.Test;

public class GenerateProxiesTest {

	@Test
	public void generateBasicProxy() {
		BasicProxyFactoryImpl basicProxyFactory = new BasicProxyFactoryImpl( SimpleEntity.class, null,
				new ByteBuddyState() );
		assertNotNull( basicProxyFactory.getProxy() );
	}

	@Test
	public void generateProxy() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		ByteBuddyProxyHelper byteBuddyProxyHelper = new ByteBuddyProxyHelper( new ByteBuddyState() );
		Class<?> proxyClass = byteBuddyProxyHelper.buildProxy( SimpleEntity.class, new Class<?>[0] );
		assertNotNull( proxyClass );
		assertNotNull( proxyClass.getConstructor().newInstance() );
	}
}
