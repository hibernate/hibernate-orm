/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.event.spi.jpa;

import java.io.Serializable;

/**
 * @author Steve Ebersole
 */
public interface CallbackRegistry extends Serializable {
	public void preCreate(Object entity);
	public boolean hasPostCreateCallbacks(Class entityClass);
	public void postCreate(Object entity);

	public boolean preUpdate(Object entity);
	public boolean hasPostUpdateCallbacks(Class entityClass);
	public void postUpdate(Object entity);

	public void preRemove(Object entity);
	public boolean hasPostRemoveCallbacks(Class entityClass);
	public void postRemove(Object entity);

	public boolean postLoad(Object entity);
}
