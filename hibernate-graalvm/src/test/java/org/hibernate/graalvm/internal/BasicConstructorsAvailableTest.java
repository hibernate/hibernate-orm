/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graalvm.internal;

import java.lang.reflect.Constructor;

import org.hibernate.internal.util.ReflectHelper;

import org.junit.Assert;
import org.junit.Test;

public class BasicConstructorsAvailableTest {

	@Test
	public void checkNonDefaultConstructorsCanBeLoaded() {
		Class[] classes = StaticClassLists.typesNeedingAllConstructorsAccessible();
		for ( Class c : classes ) {
			Constructor[] declaredConstructors = c.getDeclaredConstructors();
			Assert.assertTrue( declaredConstructors.length > 0 );
			if ( declaredConstructors.length == 1 ) {
				//If there's only one, let's check that this class wasn't placed in the wrong cathegory:
				Assert.assertTrue( declaredConstructors[0].getParameterCount() > 0 );
			}
		}
	}

	@Test
	public void checkDefaultConstructorsAreAvailable() {
		Class[] classes = StaticClassLists.typesNeedingDefaultConstructorAccessible();
		for ( Class c : classes ) {
			Constructor constructor = ReflectHelper.getDefaultConstructor( c );
			Assert.assertNotNull( "Failed for class: " + c.getName(), constructor );
		}
	}

	@Test
	public void checkArraysAreArrays() {
		Class[] classes = StaticClassLists.typesNeedingArrayCopy();
		for ( Class c : classes ) {
			Assert.assertTrue( "Wrong category for type: " + c.getName(), c.isArray() );
			Constructor[] constructors = c.getConstructors();
			Assert.assertEquals( 0, constructors.length );
		}
	}

}
