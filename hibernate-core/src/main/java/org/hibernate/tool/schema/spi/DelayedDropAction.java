/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.service.ServiceRegistry;

/**
 * Memento representing the dropping of a schema as part of create-drop
 * hbm2ddl.auto handling.  This memento is registered with the
 * SessionFactory and executed as the SessionFactory is closing.
 * <p>
 * Implementations should be Serializable
 *
 * @author Steve Ebersole
 */
public interface DelayedDropAction {
	/**
	 * Perform the delayed schema drop.
	 *
	 * @param serviceRegistry Access to the ServiceRegistry
	 */
	void perform(ServiceRegistry serviceRegistry);
}
