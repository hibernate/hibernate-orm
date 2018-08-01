package org.hibernate.bytecode.internal.bytebuddy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.spi.ByteCodeHelper;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyHelper;
import org.junit.Test;

public class GenerateProxiesTest {

	@Test
	public void generateBasicProxy() {
		BasicProxyFactoryImpl basicProxyFactory = new BasicProxyFactoryImpl( SimpleEntity.class, new Class<?>[0],
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
	public void generateEnhancedClass() throws EnhancementException, IOException {
		Enhancer enhancer = new EnhancerImpl( new DefaultEnhancementContext(), new ByteBuddyState() );
		enhancer.enhance( SimpleEntity.class.getName(),
				ByteCodeHelper.readByteCode( SimpleEntity.class.getClassLoader()
						.getResourceAsStream( SimpleEntity.class.getName().replace( '.', '/' ) + ".class" ) ) );
	}
}
