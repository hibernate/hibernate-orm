/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.service.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.service.spi.ServiceBinding;
import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit Test for ConcurrentServiceBinding
 *
 * @author Sanne Grinovero
 */
@TestForIssue(jiraKey="HHH-8947")
public class ConcurrentServiceBindingTest {

	private Class[] testTypes = new Class[]{ String.class, Integer.class, ServiceBinding.class, ConnectionProviderInitiator.class, HashMap.class,
			ConcurrentServiceBindingTest.class, Long.class, Test.class, Set.class, HashSet.class };

	@Test
	public void normalImplementationTest() {
		final ConcurrentServiceBinding<Class, String> binder = new ConcurrentServiceBinding<Class, String>();
		verifyBehaviour( binder );
	}

	@Test
	public void allKeysCollisions() {
		final ConcurrentServiceBinding<Class, String> binder = new ConcurrentServiceBinding<Class, String>() {
			protected int hashKey(final Class key) {
				return 15;
			}
		};
		verifyBehaviour( binder );
	}

	@Test
	public void someKeysCollisions() {
		final Set<Class> collidingClasses = new HashSet<Class>();
		collidingClasses.add( String.class );
		collidingClasses.add( ServiceBinding.class );
		collidingClasses.add( ConnectionProviderInitiator.class );
		final Set<Class> classedWhichHit = new HashSet<Class>();
		final ConcurrentServiceBinding<Class, String> binder = new ConcurrentServiceBinding<Class, String>() {
			protected int hashKey(final Class key) {
				if ( collidingClasses.contains( key ) ) {
					classedWhichHit.add( key );
					return 15;
				}
				else {
					return System.identityHashCode( key );
				}
			}
		};
		verifyBehaviour( binder );
		Assert.assertEquals( 3, classedWhichHit.size() );//to verify the test is being applied as expected
	}

	private void verifyBehaviour(ConcurrentServiceBinding<Class, String> binder) {
		isEmpty( binder );
		HashSet<Class> addedTypes = new HashSet<Class>();
		for ( Class newtype : testTypes ) {
			addedTypes.add( newtype );
			binder.put( newtype, newtype.toString() );
			containsExactly( binder, addedTypes );
		}
		binder.clear();
		isEmpty( binder );
	}

	private void containsExactly(ConcurrentServiceBinding<Class, String> binder, HashSet<Class> addedTypes) {
		for ( Class knownType : addedTypes ) {
			final String value = binder.get( knownType );
			Assert.assertNotNull( value );
			Assert.assertEquals( knownType.toString(), value );
			int countElements = 0;
			boolean present = false;
			for ( String each : binder.values() ) {
				countElements++;
				if ( each.equals( knownType.toString() ) ) {
					Assert.assertFalse( "should have been unique", present );
					present = true;
				}
			}
			Assert.assertEquals( addedTypes.size(), countElements );
			Assert.assertTrue( present );
		}
	}

	private void isEmpty(ConcurrentServiceBinding<Class, String> binder) {
		for ( String value : binder.values() ) {
			Assert.fail( "Expected it to be empty" );
		}
		for ( Class type : testTypes ) {
			Assert.assertNull( binder.get( type ) );
		}
	}

}
