/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.domain.internal;

import java.lang.reflect.Member;

/**
 * Represents a no-op domain model member, or a member which intentionally does not exist on the DTO and is ignored during persistence and hydration in order to prevent
 * unnecessary work (generally for performance reasons).
 *
 * @author Mike Hill
 * @see org.hibernate.property.access.internal.PropertyAccessStrategyNoopImpl
 */
public class NoopMember implements Member {

	/**
	 * Singleton access
	 */
	public static final NoopMember INSTANCE = new NoopMember();


	@Override
	public Class<?> getDeclaringClass() {
		return null;
	}


	@Override
	public String getName() {
		return null;
	}


	@Override
	public int getModifiers() {
		return 0;
	}


	@Override
	public boolean isSynthetic() {
		return false;
	}
}
