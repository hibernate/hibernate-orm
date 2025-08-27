/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.proxy.map;

import org.hibernate.proxy.AbstractSerializableProxy;

public final class SerializableMapProxy extends AbstractSerializableProxy {

	public SerializableMapProxy(
			String entityName,
			Object id,
			Boolean readOnly,
			String sessionFactoryUuid,
			String sessionFactoryName,
			boolean allowLoadOutsideTransaction) {
		super( entityName, id, readOnly, sessionFactoryUuid, sessionFactoryName, allowLoadOutsideTransaction );
	}

	private Object readResolve() {
		MapLazyInitializer initializer = new MapLazyInitializer( getEntityName(), getId(), null );
		afterDeserialization( initializer );
		return new MapProxy( initializer );
	}
}
