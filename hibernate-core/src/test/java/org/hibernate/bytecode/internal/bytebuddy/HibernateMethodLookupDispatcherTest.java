/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.bytebuddy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;

import org.junit.Test;

public class HibernateMethodLookupDispatcherTest {

	@Test
	public void testAuthorizedClass() {
		HibernateMethodLookupDispatcher.registerAuthorizedClass( AuthorizedClass.class.getName() );

		AuthorizedClass authorizedClass = new AuthorizedClass();
		assertNotNull( authorizedClass.declaredMethod );
		assertEquals( "myMethod", authorizedClass.declaredMethod.getName() );
	}

	@Test( expected = SecurityException.class )
	public void testUnauthorizedClass() {
		new UnauthorizedClass();
	}

	public static class AuthorizedClass {

		private Method declaredMethod;

		public AuthorizedClass() {
			declaredMethod = HibernateMethodLookupDispatcher.getDeclaredMethod( AuthorizedClass.class, "myMethod",
					new Class<?>[]{ String.class } );
		}

		public void myMethod(String myParameter) {
		}
	}

	public static class UnauthorizedClass {

		@SuppressWarnings("unused")
		private Method declaredMethod;

		public UnauthorizedClass() {
			declaredMethod = HibernateMethodLookupDispatcher.getDeclaredMethod( AuthorizedClass.class, "myMethod",
					new Class<?>[]{ String.class } );
		}

		public void myMethod(String myParameter) {
		}
	}
}
