/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.proxy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.internal.TcclLookupPrecedence;
import org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl;
import org.hibernate.bytecode.spi.ByteCodeHelper;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

@Jira("https://hibernate.atlassian.net/browse/HHH-14694")
public class ProxyClassReuseTest {

	@Test
	void testReuse() {
		Function<SessionFactoryImplementor, Class<?>> proxyGetter = sf -> {
			try (SessionImplementor s = sf.openSession()) {
				return s.getReference( MyEntity.class, "abc" ).getClass();
			}
		};
		final BytecodeProvider bytecodeProvider = new BytecodeProviderImpl();
		Class<?> proxyClass1 = withFactory( proxyGetter, bytecodeProvider, null );
		Class<?> proxyClass2 = withFactory( proxyGetter, bytecodeProvider, null );
		assertSame( proxyClass1, proxyClass2 );
	}

	@Test
	void testReuseWithDifferentFactories() {
		Function<SessionFactoryImplementor, Class<?>> proxyGetter = sf -> {
			try (SessionImplementor s = sf.openSession()) {
				return s.getReference( MyEntity.class, "abc" ).getClass();
			}
		};
		Class<?> proxyClass1 = withFactory( proxyGetter, null, null );
		Class<?> proxyClass2 = withFactory( proxyGetter, null, null );
		assertSame( proxyClass1, proxyClass2 );
		assertSame( proxyClass1.getClassLoader(), MyEntity.class.getClassLoader() );
		assertSame( proxyClass2.getClassLoader(), MyEntity.class.getClassLoader() );
	}

	@Test
	void testNoReuse() {
		Function<SessionFactoryImplementor, Class<?>> proxyGetter = sf -> {
			try {
				//noinspection unchecked
				Function<SessionFactoryImplementor, Class<?>> getter = (Function<SessionFactoryImplementor, Class<?>>) Thread.currentThread()
						.getContextClassLoader()
						.loadClass( ProxyClassReuseTest.class.getName() + "$ProxyGetter" )
						.getConstructor()
						.newInstance();
				return getter.apply( sf );
			}
			catch (Exception ex) {
				throw new RuntimeException( ex );
			}
		};
		// Create two isolated class loaders that load the entity and proxy classes in isolation
		Set<String> isolatedClasses = Set.of( "org.hibernate.orm.test.proxy.*" );
		ClassLoader cl1 = new IsolatingClassLoader( isolatedClasses );
		ClassLoader cl2 = new IsolatingClassLoader( isolatedClasses );
		Class<?> proxyClass1 = withFactory( proxyGetter, null, cl1 );
		Class<?> proxyClass2 = withFactory( proxyGetter, null, cl2 );
		// The two proxy classes shall be defined on the respective isolated class loaders and hence be different
		assertNotSame( proxyClass1, proxyClass2 );
		assertSame( proxyClass1.getClassLoader(), cl1 );
		assertSame( proxyClass2.getClassLoader(), cl2 );
	}

	<T> T withFactory(Function<SessionFactoryImplementor, T> consumer, BytecodeProvider bytecodeProvider, ClassLoader classLoader) {
		final ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			if (classLoader != null) {
				Thread.currentThread().setContextClassLoader( classLoader );
			}
			final BootstrapServiceRegistryBuilder bsr = new BootstrapServiceRegistryBuilder();
			bsr.applyTcclLookupPrecedence( TcclLookupPrecedence.BEFORE );
			final StandardServiceRegistryBuilder builder = ServiceRegistryUtil.serviceRegistryBuilder(bsr.build());
			if ( bytecodeProvider != null ) {
				builder.addService( BytecodeProvider.class, bytecodeProvider );
			}
			final StandardServiceRegistry ssr = builder.build();

			try (final SessionFactoryImplementor sf = (SessionFactoryImplementor) new MetadataSources( ssr )
					.addAnnotatedClassName( ProxyClassReuseTest.class.getName() + "$MyEntity" )
					.buildMetadata()
					.getSessionFactoryBuilder()
					.build()) {
				return consumer.apply( sf );
			}
			catch (Exception e) {
				StandardServiceRegistryBuilder.destroy( ssr );
				throw e;
			}
		}
		finally {
			if (classLoader != null) {
				Thread.currentThread().setContextClassLoader( oldClassLoader );
			}
		}
	}

	@Entity(name = "MyEntity")
	public static class MyEntity {
		@Id
		String id;
		String name;
	}

	public static class ProxyGetter implements Function<SessionFactoryImplementor, Class<?>> {
		@Override
		public Class<?> apply(SessionFactoryImplementor sf) {
			try (SessionImplementor s = sf.openSession()) {
				return s.getReference( MyEntity.class, "abc" ).getClass();
			}
		}
	}

	private static class IsolatingClassLoader extends ClassLoader {

		private final Set<String> isolatedClasses;

		public IsolatingClassLoader(Set<String> isolatedClasses) {
			super( IsolatingClassLoader.class.getClassLoader() );
			this.isolatedClasses = isolatedClasses;
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			synchronized (getClassLoadingLock(name)) {
				Class<?> c = findLoadedClass(name);
				if (c == null) {
					if (isIsolatedClass(name)) {
						InputStream is = this.getResourceAsStream( name.replace( '.', '/' ) + ".class" );
						if ( is == null ) {
							throw new ClassNotFoundException( name + " not found" );
						}

						try {
							byte[] bytecode = ByteCodeHelper.readByteCode( is );
							return defineClass( name, bytecode, 0, bytecode.length );
						}
						catch( Throwable t ) {
							throw new ClassNotFoundException( name + " not found", t );
						}
					} else {
						// Parent first
						c = super.loadClass(name, resolve);
					}
				}
				return c;
			}
		}

		private boolean isIsolatedClass(String name) {
			if (isolatedClasses != null) {
				for (String isolated : isolatedClasses) {
					if (isolated.endsWith(".*")) {
						String isolatedPackage = isolated.substring(0, isolated.length() - 1);
						String paramPackage = name.substring(0, name.lastIndexOf('.') + 1);
						if (paramPackage.startsWith(isolatedPackage)) {
							// Matching package
							return true;
						}
					} else if (isolated.equals(name)) {
						return true;
					}
				}
			}
			return false;
		}
	}
}
