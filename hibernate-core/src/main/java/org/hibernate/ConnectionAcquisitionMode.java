/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Indicates the manner in which JDBC Connections should be acquired.  Inverse to
 * {@link org.hibernate.ConnectionReleaseMode}.
 * <p/>
 * NOTE : Not yet used.  The only current behavior is the legacy behavior, which is
 * {@link #AS_NEEDED}.
 *
 * @author Steve Ebersole
 */
public enum ConnectionAcquisitionMode {
	/**
	 * The Connection will be acquired as soon as the Hibernate Session is opened.  This
	 * also circumvents ConnectionReleaseMode, as the Connection will then be held until the
	 * Session is closed.
	 */
	IMMEDIATELY,
	/**
	 * The legacy behavior.  A Connection is only acquired when (if_) it is actually needed.
	 */
	AS_NEEDED,
	/**
	 * Not sure yet tbh :)
	 */
	DEFAULT
}
