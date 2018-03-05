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
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.reflection.java.JavaXMember;

/**
 * Manage the various fun-ness of dealing with HCANN...
 *
 * @author Steve Ebersole
 */
public class HCANNHelper {
	private static Method getMemberMethod;
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
}
