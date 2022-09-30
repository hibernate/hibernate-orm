/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.action.internal;

/**
 * With this interface we can compare entity actions in the queue
 * even if the implementation doesn't extend {@link EntityAction}.
 */
public interface ComparableEntityAction extends Comparable<ComparableEntityAction> {
	String getEntityName();

	Object getId();
}
