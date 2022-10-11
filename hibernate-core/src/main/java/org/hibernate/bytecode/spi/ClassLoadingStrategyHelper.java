/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.spi;

import java.lang.invoke.MethodHandles;

import org.hibernate.HibernateException;

import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

import static org.hibernate.internal.CoreLogging.messageLogger;

public class ClassLoadingStrategyHelper {

	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

	public static ClassLoadingStrategy<ClassLoader> resolveClassLoadingStrategy(Class<?> originalClass) {
		try {
			return ClassLoadingStrategy.UsingLookup.of( MethodHandles.privateLookupIn( originalClass, LOOKUP ) );
		}
		catch (Throwable e) {
			throw new HibernateException( messageLogger( ClassLoadingStrategyHelper.class ).bytecodeEnhancementFailedUnableToGetPrivateLookupFor( originalClass.getName() ), e );
		}
	}

}
