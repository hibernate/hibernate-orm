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
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

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
		Class<?> proxyClass1 = withFactory( proxyGetter, bytecodeProvider );
		Class<?> proxyClass2 = withFactory( proxyGetter, bytecodeProvider );
		assertSame( proxyClass1, proxyClass2 );
	}

	@Test
	void testNoReuse() {
		Function<SessionFactoryImplementor, Class<?>> proxyGetter = sf -> {
			try (SessionImplementor s = sf.openSession()) {
				return s.getReference( MyEntity.class, "abc" ).getClass();
			}
		};
		Class<?> proxyClass1 = withFactory( proxyGetter, null );
		Class<?> proxyClass2 = withFactory( proxyGetter, null );
		assertNotSame( proxyClass1, proxyClass2 );
	}

	<T> T withFactory(Function<SessionFactoryImplementor, T> consumer, BytecodeProvider bytecodeProvider) {
		final StandardServiceRegistryBuilder builder = ServiceRegistryUtil.serviceRegistryBuilder();
		if ( bytecodeProvider != null ) {
			builder.addService( BytecodeProvider.class, bytecodeProvider );
		}
		final StandardServiceRegistry ssr = builder.build();

		try (final SessionFactoryImplementor sf = (SessionFactoryImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( MyEntity.class )
				.buildMetadata()
				.getSessionFactoryBuilder()
				.build()) {
			return consumer.apply( sf );
		}
		catch ( Exception e ) {
			StandardServiceRegistryBuilder.destroy( ssr );
			throw e;
		}
	}

	@Entity(name = "MyEntity")
	public static class MyEntity {
		@Id
		String id;
		String name;
	}
}
