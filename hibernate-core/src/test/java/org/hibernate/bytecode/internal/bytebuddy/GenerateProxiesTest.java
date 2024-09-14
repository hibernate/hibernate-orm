/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.bytebuddy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.spi.ByteCodeHelper;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.property.access.internal.PropertyAccessStrategyFieldImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyHelper;
import org.hibernate.testing.orm.junit.JiraKey;
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

	@Test
	public void generateFastClassAndReflectionOptimizer() {
		BytecodeProviderImpl bytecodeProvider = new BytecodeProviderImpl();
		ReflectionOptimizer reflectionOptimizer = bytecodeProvider.getReflectionOptimizer( SimpleEntity.class,
				new String[]{ "getId", "getName" }, new String[]{ "setId", "setName" },
				new Class<?>[]{ Long.class, String.class } );
		assertEquals( 2, reflectionOptimizer.getAccessOptimizer().getPropertyNames().length );
		assertNotNull( reflectionOptimizer.getInstantiationOptimizer().newInstance() );
	}

	@Test
	@JiraKey("HHH-16772")
	public void generateFastMappedSuperclassAndReflectionOptimizer() {
		BytecodeProviderImpl bytecodeProvider  = new BytecodeProviderImpl();
		final Map<String, PropertyAccess> propertyAccessMap = new LinkedHashMap<>();

		final PropertyAccessStrategyFieldImpl propertyAccessStrategy = new PropertyAccessStrategyFieldImpl();

		propertyAccessMap.put(
				"timestamp",
				propertyAccessStrategy.buildPropertyAccess(MappedSuperclassEntity.class, "value", true )
		);

		ReflectionOptimizer reflectionOptimizer = bytecodeProvider.getReflectionOptimizer(
				MappedSuperclassEntity.class,
				propertyAccessMap
		);

		assertNotNull(reflectionOptimizer);
		assertEquals( 1, reflectionOptimizer.getAccessOptimizer().getPropertyNames().length );
		assertNotNull( reflectionOptimizer.getInstantiationOptimizer().newInstance() );
	}

	@Test
	public void generateEnhancedClass() throws EnhancementException, IOException {
		Enhancer enhancer = new EnhancerImpl( new DefaultEnhancementContext(), new ByteBuddyState() );
		enhancer.enhance( SimpleEntity.class.getName(),
				ByteCodeHelper.readByteCode( SimpleEntity.class.getClassLoader()
						.getResourceAsStream( SimpleEntity.class.getName().replace( '.', '/' ) + ".class" ) ) );
	}
}
