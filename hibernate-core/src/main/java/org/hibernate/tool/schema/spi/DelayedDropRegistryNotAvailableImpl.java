/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

/**
 * Implementation of DelayedDropRegistry for cases when the delayed-drop portion of
 * "create-drop" is not valid.
 *
 * @author Steve Ebersole
 */
public class DelayedDropRegistryNotAvailableImpl implements DelayedDropRegistry {
	/**
	 * Singleton access
	 */
	public static final DelayedDropRegistryNotAvailableImpl INSTANCE = new DelayedDropRegistryNotAvailableImpl();

	@Override
	public void registerOnCloseAction(DelayedDropAction action) {
		throw new SchemaManagementException(
				"DelayedDropRegistry is not available in this context.  'create-drop' action is not valid"
		);
	}
}
