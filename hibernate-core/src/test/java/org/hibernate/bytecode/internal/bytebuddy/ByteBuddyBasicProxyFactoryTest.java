/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal.bytebuddy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

public class ByteBuddyBasicProxyFactoryTest {

	private static final BasicProxyFactoryImpl BASIC_PROXY_FACTORY = new BasicProxyFactoryImpl( Entity.class, null, new ByteBuddyState() );

	@Test
	@JiraKey(value = "HHH-12786")
	public void testEqualsHashCode() {
		Object entityProxy = BASIC_PROXY_FACTORY.getProxy();

		assertTrue( entityProxy.equals( entityProxy ) );
		assertNotEquals(0, entityProxy.hashCode() );

		Object otherEntityProxy = BASIC_PROXY_FACTORY.getProxy();
		assertFalse( entityProxy.equals( otherEntityProxy ) );
	}

	@Test
	@JiraKey(value = "HHH-12786")
	public void testToString() {
		Object entityProxy = BASIC_PROXY_FACTORY.getProxy();

		assertTrue( entityProxy.toString().contains( "HibernateBasicProxy" ) );
	}

	@Test
	@JiraKey(value = "HHH-12786")
	public void testGetterSetter() {
		Entity entityProxy = (Entity) BASIC_PROXY_FACTORY.getProxy();

		entityProxy.setBool( true );
		assertTrue( entityProxy.isBool() );
		entityProxy.setBool( false );
		assertFalse( entityProxy.isBool() );

		entityProxy.setString( "John Irving" );
		assertEquals( "John Irving", entityProxy.getString() );
	}

	@Test
	@JiraKey(value = "HHH-12786")
	public void testNonGetterSetterMethod() {
		Entity entityProxy = (Entity) BASIC_PROXY_FACTORY.getProxy();

		assertNull( entityProxy.otherMethod() );
	}

	@Test
	@JiraKey(value = "HHH-13915")
	public void testProxiesDoNotShareState() {
		Entity entityAProxy = (Entity) BASIC_PROXY_FACTORY.getProxy();
		entityAProxy.setString( "John Irving" );

		Entity entityBProxy = (Entity) BASIC_PROXY_FACTORY.getProxy();
		assertNull( entityBProxy.getString() );
	}

	public static class Entity {

		private String string;

		private boolean bool;

		public Entity() {
		}

		public boolean isBool() {
			return bool;
		}

		public void setBool(boolean bool) {
			this.bool = bool;
		}

		public String getString() {
			return string;
		}

		public void setString(String string) {
			this.string = string;
		}

		public String otherMethod() {
			return "a string";
		}
	}
}
