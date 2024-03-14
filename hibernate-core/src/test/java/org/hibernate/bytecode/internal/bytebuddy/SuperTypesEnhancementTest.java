/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.bytebuddy;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import org.hibernate.LazyInitializationException;
import org.hibernate.engine.spi.PrimeAmongSecondarySupertypes;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyFactory;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyHelper;

import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Verifies that proxies being generated by ByteBuddyProxyHelper
 * do not break the contract with PrimeAmongSecondarySupertypes.
 * A problem in this are could obviously manifest as a semantic
 * issue, but could also manifest solely as a performance issue;
 * therefore we check implementation details via unit tests
 * rather than the typical integration test we'd have in such cases.
 *
 * @author Sanne Grinovero
 */
@TestForIssue( jiraKey = "HHH-15790" )
public class SuperTypesEnhancementTest {

	private static final ByteBuddyProxyHelper helper = new ByteBuddyProxyHelper( new ByteBuddyState() );

	private static Stream<Arguments> superTypeMethods() {
		return Arrays.stream( PrimeAmongSecondarySupertypes.class.getDeclaredMethods() )
				// need to filter out methods added by jacoco
				.filter( method -> !method.isSynthetic() )
				.map( Arguments::of );
	}

	private static Stream<Arguments> interfaces() {
		return Arrays.stream( PrimeAmongSecondarySupertypes.class.getDeclaredMethods() )
				// need to filter out methods added by jacoco
				.filter( method -> !method.isSynthetic() )
				.map( m -> m.getReturnType() )
				.map( e -> Arguments.of( e ) );
	}

	@ParameterizedTest
	@MethodSource("superTypeMethods")
	public void testNamingConventions(Method m) {
		final Class<?> returnType = m.getReturnType();
		final String expectedMethodName = "as" + returnType.getSimpleName();
		Assert.assertEquals( expectedMethodName, m.getName() );
		Assert.assertNotNull( m.isDefault() );
	}

	@ParameterizedTest
	@MethodSource("superTypeMethods")
	public void testAllsubInterfacesExtendTheSingleparent(Method m) {
		final Class<?> returnType = m.getReturnType();
		Assert.assertTrue( PrimeAmongSecondarySupertypes.class.isAssignableFrom( returnType ) );
	}

	@ParameterizedTest
	@MethodSource("superTypeMethods")
	public void testSubInterfaceOverrides(Method m) throws NoSuchMethodException {
		final Class<?> returnType = m.getReturnType();
		final Method subMethod = returnType.getMethod( m.getName(), m.getParameterTypes() );
		Assert.assertNotNull( subMethod );
		Assert.assertNotNull( subMethod.isDefault() );
	}

	@Test
	public void testHibernateProxyGeneration() {
		ProxyFactory enhancer = createProxyFactory( SampleClass.class, HibernateProxy.class );
		final Object proxy = enhancer.getProxy( Integer.valueOf( 1 ), null );
		Assert.assertTrue( HibernateProxy.class.isAssignableFrom( proxy.getClass() ) );
		Assert.assertTrue( proxy instanceof HibernateProxy );
		PrimeAmongSecondarySupertypes casted = (PrimeAmongSecondarySupertypes) proxy;
		final HibernateProxy extracted = casted.asHibernateProxy();
		Assert.assertNotNull( extracted );
		Assert.assertSame( proxy, extracted );
		testForLIE( (SampleClass) proxy );
	}

	/**
	 * Self-check: verify that this is in fact a lazy proxy
	 */
	private void testForLIE(SampleClass sampleProxy) {
		SampleClass other = new SampleClass();
		Assert.assertEquals( 7, other.additionMethod( 3,4 ) );
		Assert.assertThrows( LazyInitializationException.class, () -> sampleProxy.additionMethod( 3, 4 ) );
	}

	private ProxyFactory createProxyFactory(Class<?> persistentClass, Class<?>... interfaces) {
		ByteBuddyProxyFactory proxyFactory = new ByteBuddyProxyFactory( helper );
		proxyFactory.postInstantiate( "", persistentClass, Set.of( interfaces ), null, null, null );
		return proxyFactory;
	}

	//Just a class with some fields and methods to proxy
	static class SampleClass {
		int intField;
		String stringField;

		public int additionMethod(int a, int b) {
			return a + b;
		}
	}

}
