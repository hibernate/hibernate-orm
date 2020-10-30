/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XPackage;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.annotations.common.reflection.java.JavaXMember;

/**
 * Manage the various fun-ness of dealing with HCANN...
 *
 * @deprecated all these methods are available only to maintain compatibility with older versions of HCANN - stop using
 * @author Steve Ebersole
 */
@Deprecated
public class HCANNHelper {
	private static final Method getMemberMethod;
	private static final Method RESET_REFLECTIONMANAGER_METHOD; // might not exist: null in such case.
	private static final Method TOXPACKAGE_REFLECTIONMANAGER_METHOD;
	static {
		// The following is in a static block to avoid problems lazy-initializing
		// and making accessible in a multi-threaded context. See HHH-11289.
		final Class<?> javaXMemberClass = JavaXMember.class;
		try {
			getMemberMethod = javaXMemberClass.getDeclaredMethod( "getMember" );
			// NOTE : no need to check accessibility here - we know it is protected
			getMemberMethod.setAccessible( true );
		}
		catch (NoSuchMethodException e) {
			throw new AssertionFailure(
					"Could not resolve JavaXMember#getMember method in order to access XProperty member signature",
					e
			);
		}
		catch (Exception e) {
			throw new HibernateException( "Could not access org.hibernate.annotations.common.reflection.java.JavaXMember#getMember method", e );
		}
		Method resetMethod;
		try {
			resetMethod = ReflectionManager.class.getDeclaredMethod( "reset" );
		}
		catch (NoSuchMethodException e) {
			//Ignore this: we want to be compatible with older versions which don't have this.
			resetMethod = null;
		}
		RESET_REFLECTIONMANAGER_METHOD = resetMethod;

		Method toXpackageMethod;
		try {
			toXpackageMethod = JavaReflectionManager.class.getDeclaredMethod( "getXAnnotatedElement", Package.class );
			toXpackageMethod.setAccessible( true );
		}
		catch (NoSuchMethodException e) {
			throw new AssertionFailure( "org.hibernate.annotations.common.reflection.java.JavaReflectionManager#getXAnnotatedElement method", e );
		}
		TOXPACKAGE_REFLECTIONMANAGER_METHOD = toXpackageMethod;
	}

	public static String annotatedElementSignature(XProperty xProperty) {
		return getUnderlyingMember( xProperty ).toString();
	}

	public static Member getUnderlyingMember(XProperty xProperty) {
		try {
			return (Member) getMemberMethod.invoke( xProperty );
		}
		catch (IllegalAccessException e) {
			throw new AssertionFailure(
					"Could not resolve member signature from XProperty reference",
					e
			);
		}
		catch (InvocationTargetException e) {
			throw new AssertionFailure(
					"Could not resolve member signature from XProperty reference",
					e.getCause()
			);
		}
	}

	/**
	 * Attempts to invoke the reset() method on the ReflectionManager.
	 * Some older versions of HCANN don't have this method, in which case we
	 * simply won't invoke the reset method.
	 * If the method doesn't exist, no errors will be propagated.
	 * @param reflectionManager
	 * @throws HibernateException if the method seems to exist and still we fail to invoke it.
	 */
	public static void resetIfResetMethodExists(ReflectionManager reflectionManager) {
		if ( RESET_REFLECTIONMANAGER_METHOD != null ) {
			try {
				RESET_REFLECTIONMANAGER_METHOD.invoke( reflectionManager );
			}
			catch (InvocationTargetException | IllegalAccessException e) {
				throw new HibernateException( "Could not invoke method " + RESET_REFLECTIONMANAGER_METHOD, e );
			}
		}
	}

	public static XPackage toXPackage(final ReflectionManager reflectionManager, final Package packaze) {
		if ( ! ( reflectionManager instanceof JavaReflectionManager ) ) {
			throw new AssertionFailure( "We expect an instance of JavaReflectionManager here" );
		}
		// on HCANN 5.1.1 we have both options:
		//    public XPackage toXPackage(Package pkg);
		//    XPackage getXAnnotatedElement(Package pkg);
		// on HCANN 5.1.0 we only have:
		//    XPackage getXAnnotatedElement(Package pkg);
		// N.B. the one method which they have in common is not public.
		try {
			return (XPackage) TOXPACKAGE_REFLECTIONMANAGER_METHOD.invoke( reflectionManager, packaze );
		}
		catch (InvocationTargetException | IllegalAccessException e) {
			throw new HibernateException( "Could not invoke method " + TOXPACKAGE_REFLECTIONMANAGER_METHOD, e );
		}
	}

}
