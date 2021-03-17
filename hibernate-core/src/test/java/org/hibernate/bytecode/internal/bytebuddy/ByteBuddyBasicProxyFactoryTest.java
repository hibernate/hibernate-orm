/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.bytebuddy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.hibernate.bytecode.internal.bytebuddy.BasicProxyFactoryImpl;
import org.hibernate.bytecode.internal.bytebuddy.ByteBuddyState;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-12786")
public class ByteBuddyBasicProxyFactoryTest {

	private static final BasicProxyFactoryImpl BASIC_PROXY_FACTORY = new BasicProxyFactoryImpl( Entity.class, new Class[0], new ByteBuddyState() );

	@Test
	public void testEqualsHashCode() {
		Object entityProxy = BASIC_PROXY_FACTORY.getProxy();

		assertTrue( entityProxy.equals( entityProxy ) );
		assertNotNull( entityProxy.hashCode() );

		Object otherEntityProxy = BASIC_PROXY_FACTORY.getProxy();
		assertFalse( entityProxy.equals( otherEntityProxy ) );
	}

	@Test
	public void testToString() {
		Object entityProxy = BASIC_PROXY_FACTORY.getProxy();

		assertTrue( entityProxy.toString().contains( "HibernateBasicProxy" ) );
	}

	@Test
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
	public void testNonGetterSetterMethod() {
		Entity entityProxy = (Entity) BASIC_PROXY_FACTORY.getProxy();

		assertNull( entityProxy.otherMethod() );
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
