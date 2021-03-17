/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.lang.reflect.Member;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.reflection.java.JavaXMember;

/**
 * Manage the various fun-ness of dealing with HCANN...
 *
 * @author Steve Ebersole
 */
public final class HCANNHelper {

	/**
	 * @deprecated Prefer using {@link #annotatedElementSignature(JavaXMember)}
	 */
	@Deprecated
	public static String annotatedElementSignature(XProperty xProperty) {
		return getUnderlyingMember( xProperty ).toString();
	}

	public static String annotatedElementSignature(final JavaXMember jxProperty) {
		return getUnderlyingMember( jxProperty ).toString();
	}

	/**
	 * @deprecated Prefer using {@link #getUnderlyingMember(JavaXMember)}
	 */
	@Deprecated
	public static Member getUnderlyingMember(XProperty xProperty) {
		if (xProperty instanceof JavaXMember) {
			JavaXMember jx = (JavaXMember)xProperty;
			return jx.getMember();
		}
		else {
			throw new org.hibernate.HibernateException( "Can only extract Member from a XProperty which is a JavaXMember" );
		}
	}

	public static Member getUnderlyingMember(final JavaXMember jxProperty) {
		return jxProperty.getMember();
	}
}
