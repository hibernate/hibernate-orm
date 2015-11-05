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
	void preCreate(Object entity);
	boolean hasPostCreateCallbacks(Class entityClass);
	void postCreate(Object entity);

	boolean preUpdate(Object entity);
	boolean hasPostUpdateCallbacks(Class entityClass);
	void postUpdate(Object entity);

	void preRemove(Object entity);
	boolean hasPostRemoveCallbacks(Class entityClass);
	void postRemove(Object entity);

	boolean postLoad(Object entity);

	boolean hasRegisteredCallbacks(Class entityClass, Class annotationClass);
}
