/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.validation;

import net.bytebuddy.ByteBuddy;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static net.bytebuddy.implementation.FixedValue.value;
import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.returns;

/**
 * @author Gavin King
 */
@FunctionalInterface
public interface Mocker<T> {

	T make(Object... args);

	Map<Class<?>, Class<?>> mocks = new HashMap<>();

	static <T> Supplier<T> nullary(Class<T> clazz) {
		try {
			Class<? extends T> mock = load(clazz);
			return () -> {
				try {
					return mock.newInstance();
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			};
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	static <T> Mocker<T> variadic(Class<T> clazz) {
		Constructor<?>[] constructors = load(clazz).getDeclaredConstructors();
		if (constructors.length>1) {
			throw new RuntimeException("more than one constructor for " + clazz);
		}
		Constructor<?> constructor = constructors[0];
		return (args) -> {
			try {
				return (T) constructor.newInstance(args);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
	}

	@SuppressWarnings("unchecked")
	static <T> Class<? extends T> load(Class<T> clazz) {
		if (mocks.containsKey(clazz)) {
			return (Class<? extends T>) mocks.get(clazz);
		}
		Class<? extends T> mock =
				new ByteBuddy()
						.subclass(clazz)
						.method(returns(String.class).and(isAbstract()))
								.intercept(value(""))
						.method(returns(boolean.class).and(isAbstract()))
								.intercept(value(false))
						.method(returns(int.class).and(isAbstract()))
								.intercept(value(0))
						.method(returns(long.class).and(isAbstract()))
								.intercept(value(0L))
						.method(returns(int[].class).and(isAbstract()))
								.intercept(value(new int[0]))
						.method(returns(String[].class).and(isAbstract()))
								.intercept(value(new String[0]))
						.make()
						.load(clazz.getClassLoader())
						.getLoaded();
		mocks.put(clazz,mock);
		return mock;
	}
}
