/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.util.Collection;
import java.util.Map;

import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatementObserver;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

/// Internal product for constructor values that can be prepared before the
/// `SessionFactoryImpl` instance and its service registry exist.
///
/// @since 9.0
/// @author Steve Ebersole
public record SessionFactoryRuntimeComponents(
		TypeConfiguration typeConfiguration,
		StatementObserver statementObserver,
		SessionFactoryObserver[] sessionFactoryObservers,
		Map<String, FilterDefinition> filterDefinitions,
		Collection<FilterDefinition> autoEnabledFilters,
		JavaType<Object> tenantIdentifierJavaType) {

	public SessionFactoryRuntimeComponents {
		sessionFactoryObservers = sessionFactoryObservers == null
				? new SessionFactoryObserver[0]
				: sessionFactoryObservers.clone();
	}

	@Override
	public SessionFactoryObserver[] sessionFactoryObservers() {
		return sessionFactoryObservers.clone();
	}
}
