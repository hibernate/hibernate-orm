/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.spi;

/**
 * See the JPA notion of joining a transaction for details.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public enum JoinStatus {
	NOT_JOINED,
	MARKED_FOR_JOINED,
	JOINED
}
