/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.reflection.java.JavaXMember;

/**
 * Manage the various fun-ness of dealing with HCANN...
 *
 * @author Steve Ebersole
 */
public class HCANNHelper {
	private static Class javaXMemberClass = JavaXMember.class;
	private static Method getMemberMethod;

	public static String annotatedElementSignature(XProperty xProperty) {
		if ( getMemberMethod == null ) {
			resolveGetMemberMethod();
		}

		return getUnderlyingMember( xProperty ).toString();
	}

	public static Member getUnderlyingMember(XProperty xProperty) {
		if ( getMemberMethod == null ) {
			resolveGetMemberMethod();
		}

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

	@SuppressWarnings("unchecked")
	private static void resolveGetMemberMethod() {
		try {
			getMemberMethod = javaXMemberClass.getDeclaredMethod( "getMember" );
			getMemberMethod.setAccessible( true );
		}
		catch (NoSuchMethodException e) {
			throw new AssertionFailure(
					"Could not resolve JavaXAnnotatedElement#toAnnotatedElement method in order to access XProperty member signature",
					e
			);
		}
	}
}
