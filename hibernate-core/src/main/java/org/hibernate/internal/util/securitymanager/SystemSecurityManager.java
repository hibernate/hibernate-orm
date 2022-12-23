/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.securitymanager;

/**
 * Encapsulates access to {@link System#getSecurityManager()},
 * in preparation of it being phased out by the JDK.
 *
 * Since JDK 17 the security manager can be disabled by setting
 * the system property {@code java.security.manager} to {@code disallow};
 * to prepare for this we also offer the option of setting
 * {@code org.hibernate.internal.util.securitymanager.FULLY_DISABLE} to {@code true}
 * to have the same effect, although limited to the Hibernate ORM code.
 */
public final class SystemSecurityManager {

	public static final String FULLY_DISABLE_PROP_NAME = "org.hibernate.internal.util.securitymanager.FULLY_DISABLE";
	private static final boolean disabledForced = Boolean.getBoolean( FULLY_DISABLE_PROP_NAME );

	private static final boolean SM_IS_ENABLED = (!disabledForced) && (System.getSecurityManager() != null );

	public static boolean isSecurityManagerEnabled() {
		return SM_IS_ENABLED;
	}

	//N.B. do not expose a "doPrivileged" helper as that would introduce a security problem

}
