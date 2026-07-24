/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.util.Objects;

import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.pipeline.internal.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.pipeline.internal.settings.ResolvedMappingSettings;
import org.hibernate.boot.pipeline.spi.ResolvedSessionFactorySettings;
import org.hibernate.service.ServiceRegistry;

/// Resolved inputs for one-shot SessionFactory bootstrap.
///
/// @since 9.0
/// @author Steve Ebersole
public record BootstrapPipelineRequest(
		ResolvedBootstrapSettings bootstrapSettings,
		ResolvedMappingSettings mappingSettings,
		MappingSources mappingSources,
		MappingCustomizations mappingCustomizations,
		FunctionRegistryCustomizations functionCustomizations,
		ResolvedSessionFactorySettings sessionFactorySettings,
		ServiceRegistry serviceRegistry,
		SessionFactoryObserver[] additionalSessionFactoryObservers) {

	public BootstrapPipelineRequest {
		Objects.requireNonNull( bootstrapSettings );
		Objects.requireNonNull( mappingSettings );
		Objects.requireNonNull( mappingSources );
		Objects.requireNonNull( sessionFactorySettings );
		Objects.requireNonNull( serviceRegistry );
		mappingCustomizations = mappingCustomizations == null ? MappingCustomizations.NONE : mappingCustomizations;
		functionCustomizations = functionCustomizations == null
				? FunctionRegistryCustomizations.NONE
				: functionCustomizations;
		additionalSessionFactoryObservers = additionalSessionFactoryObservers == null
				? new SessionFactoryObserver[0]
				: additionalSessionFactoryObservers.clone();
	}

	@Override
	public SessionFactoryObserver[] additionalSessionFactoryObservers() {
		return additionalSessionFactoryObservers.clone();
	}
}
