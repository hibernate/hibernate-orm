/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.proxy;

import java.io.Serializable;

/**
 * Convenience base class for the serialized form of {@link AbstractLazyInitializer}.
 *
 * @author Gail Badner
 */
public abstract class AbstractSerializableProxy implements Serializable {
	private final String entityName;
	private final Object id;
	private final Boolean readOnly;
	protected final String sessionFactoryUuid;
	protected final String sessionFactoryName;
	private final boolean allowLoadOutsideTransaction;

	protected AbstractSerializableProxy(
			String entityName,
			Object id,
			Boolean readOnly,
			String sessionFactoryUuid,
			String sessionFactoryName,
			boolean allowLoadOutsideTransaction) {
		this.entityName = entityName;
		this.id = id;
		this.readOnly = readOnly;
		this.sessionFactoryUuid = sessionFactoryUuid;
		this.sessionFactoryName = sessionFactoryName;
		this.allowLoadOutsideTransaction = allowLoadOutsideTransaction;
	}

	protected String getEntityName() {
		return entityName;
	}

	protected Object getId() {
		return id;
	}

	/**
	 * Initialize an {@link AbstractLazyInitializer} after deserialization.
	 *
	 * This method should only be called during deserialization,
	 * before associating the AbstractLazyInitializer with a session.
	 *
	 * @param li the {@link AbstractLazyInitializer} to initialize.
	 */
	protected void afterDeserialization(AbstractLazyInitializer li) {
		li.afterDeserialization( readOnly, sessionFactoryUuid, sessionFactoryName, allowLoadOutsideTransaction );
	}
}
