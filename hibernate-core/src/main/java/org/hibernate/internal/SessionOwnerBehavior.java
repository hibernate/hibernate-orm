/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

/**
 * @author Steve Ebersole
 */
public enum SessionOwnerBehavior {
	// todo : (5.2) document differences in regard to SessionOwner implementations
	LEGACY_JPA,
	LEGACY_NATIVE
}
