/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.proxy.map;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.proxy.AbstractLazyInitializer;

/**
 * Lazy initializer for "dynamic-map" entity representations.
 *
 * @author Gavin King
 */
public class MapLazyInitializer extends AbstractLazyInitializer implements Serializable {

	MapLazyInitializer(String entityName, Object id, SharedSessionContractImplementor session) {
		super( entityName, id, session );
	}

	public Map getMap() {
		return (Map) getImplementation();
	}

	public Class<?> getPersistentClass() {
		throw new UnsupportedOperationException("dynamic-map entity representation");
	}

	@Override
	public Class<?> getImplementationClass() {
		throw new UnsupportedOperationException("dynamic-map entity representation");
	}

	// Expose the following methods to MapProxy by overriding them (so that classes in this package see them)

	@Override
	protected void prepareForPossibleLoadingOutsideTransaction() {
		super.prepareForPossibleLoadingOutsideTransaction();
	}

	@Override
	protected boolean isAllowLoadOutsideTransaction() {
		return super.isAllowLoadOutsideTransaction();
	}

	@Override
	protected String getSessionFactoryUuid() {
		return super.getSessionFactoryUuid();
	}

	@Override
	protected String getSessionFactoryName() {
		return super.getSessionFactoryName();
	}
}
