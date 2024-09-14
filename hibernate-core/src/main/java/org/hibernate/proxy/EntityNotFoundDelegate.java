/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxy;

/**
 * Delegate to handle the scenario of an entity not found by a specified id.
 *
 * @see org.hibernate.cfg.Configuration#setEntityNotFoundDelegate(EntityNotFoundDelegate)
 * @see org.hibernate.boot.SessionFactoryBuilder#applyEntityNotFoundDelegate(EntityNotFoundDelegate)
 *
 * @author Steve Ebersole
 */
public interface EntityNotFoundDelegate {
	void handleEntityNotFound(String entityName, Object id);
}
