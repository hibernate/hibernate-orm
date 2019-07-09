/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.tuplizer.bytebuddysubclass;

import java.io.Serializable;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.tuple.Instantiator;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.matcher.ElementMatchers;

import static org.hibernate.bytecode.spi.ClassLoadingStrategyHelper.resolveClassLoadingStrategy;

/**
 * @author Florian Bien
 */
public class MyEntityInstantiator implements Instantiator {
	private final PersistentClass persistentClass;

	public MyEntityInstantiator(PersistentClass persistentClass) {
		this.persistentClass = persistentClass;
	}

	@Override
	public Object instantiate(Serializable id) {
		return instantiate();
	}

	@Override
	public Object instantiate() {
		return createInstance( persistentClass.getMappedClass() );
	}

	public static <E> E createInstance(Class<E> entityClass) {
		Class<? extends E> loaded = new ByteBuddy()
				.subclass( entityClass )
				.method( ElementMatchers.named( "toString" ) )
				.intercept( FixedValue.value( "transformed" ) )
				.make()
				// we use our internal helper to get a class loading strategy suitable for the JDK used
				.load( entityClass.getClassLoader(), resolveClassLoadingStrategy( entityClass ) )
				.getLoaded();

		try {
			return loaded.newInstance();
		}
		catch (Exception e) {
			throw new RuntimeException( "Unable to create new instance of " + entityClass.getSimpleName(), e );
		}
	}

	@Override
	public boolean isInstance(Object object) {
		return true;
	}
}
