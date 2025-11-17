/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
