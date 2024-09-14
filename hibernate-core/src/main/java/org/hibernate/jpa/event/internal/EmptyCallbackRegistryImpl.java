/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.event.internal;

import org.hibernate.jpa.event.spi.CallbackRegistry;
import org.hibernate.jpa.event.spi.CallbackType;

final class EmptyCallbackRegistryImpl implements CallbackRegistry {

	@Override
	public boolean hasRegisteredCallbacks(final Class<?> entityClass, final CallbackType callbackType) {
		return false;
	}

	@Override
	public void preCreate(final Object entity) {
		//no-op
	}

	@Override
	public void postCreate(final Object entity) {
		//no-op
	}

	@Override
	public boolean preUpdate(final Object entity) {
		return false;
	}

	@Override
	public void postUpdate(final Object entity) {
		//no-op
	}

	@Override
	public void preRemove(final Object entity) {
		//no-op
	}

	@Override
	public void postRemove(final Object entity) {
		//no-op
	}

	@Override
	public boolean postLoad(final Object entity) {
		return false;
	}

	@Override
	public void release() {
		//no-op
	}

}
