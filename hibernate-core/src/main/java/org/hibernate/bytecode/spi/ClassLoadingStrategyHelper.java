/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.spi;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hibernate.HibernateException;
import org.hibernate.internal.CoreMessageLogger;

import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

import static org.hibernate.internal.CoreLogging.messageLogger;

public class ClassLoadingStrategyHelper {

	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	private static final CoreMessageLogger LOG = messageLogger( ClassLoadingStrategyHelper.class );

	public static ClassLoadingStrategy<ClassLoader> resolveClassLoadingStrategy(Class<?> originalClass) {
		// This is available only for JDK 9+
		if ( !ClassInjector.UsingLookup.isAvailable() ) {
			return new ClassLoadingStrategy.ForUnsafeInjection( originalClass.getProtectionDomain() );
		}

		Method privateLookupIn;
		try {
			privateLookupIn = MethodHandles.class.getMethod( "privateLookupIn", Class.class, MethodHandles.Lookup.class );
		}
		catch (Exception e) {
			throw new HibernateException( LOG.bytecodeEnhancementFailed( originalClass.getName() ), e );
		}

		try {
			Object privateLookup;

			try {
				privateLookup = privateLookupIn.invoke( null, originalClass, LOOKUP );
			}
			catch (InvocationTargetException exception) {
				if ( exception.getCause() instanceof IllegalAccessException ) {
					return new ClassLoadingStrategy.ForUnsafeInjection( originalClass.getProtectionDomain() );
				}
				else {
					throw new HibernateException( LOG.bytecodeEnhancementFailed( originalClass.getName() ), exception.getCause() );
				}
			}

			return ClassLoadingStrategy.UsingLookup.of( privateLookup );
		}
		catch (Throwable e) {
			throw new HibernateException( LOG.bytecodeEnhancementFailedUnableToGetPrivateLookupFor( originalClass.getName() ), e );
		}
	}

}
