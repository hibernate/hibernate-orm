/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.util.Objects;

import org.hibernate.boot.beanvalidation.BeanValidationPlan;
import org.hibernate.boot.pipeline.spi.ResolvedSessionFactorySettings;
import org.hibernate.boot.pipeline.spi.SessionFactoryConstructionIdentity;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.metamodel.internal.RuntimeMappingHandoff;
import org.hibernate.metamodel.spi.DelayedSessionFactoryReference;

/// Internal handoff for ORM's default SessionFactory construction path.
///
/// This keeps the public producer request small while allowing ORM's default
/// factory to receive resolved construction products as a single parameter.
///
/// @since 9.0
/// @author Steve Ebersole
public record SessionFactoryConstructionPlan(
		MetadataImplementor metadata,
		ResolvedSessionFactorySettings resolvedSettings,
		SessionFactoryConstructionIdentity identity,
		SessionFactoryOptions options,
		RuntimeMappingHandoff runtimeMappingHandoff,
		SessionFactoryRuntimeComponents runtimeComponents,
		StandardServiceComponents standardServiceComponents,
		DelayedSessionFactoryReference sessionFactoryReference,
		BeanValidationPlan beanValidationPlan,
		SessionFactoryIntegratorLifecycle integratorLifecycle) {

	public SessionFactoryConstructionPlan {
		Objects.requireNonNull( metadata );
		Objects.requireNonNull( options );
		Objects.requireNonNull( runtimeMappingHandoff );
		Objects.requireNonNull( runtimeComponents );
		Objects.requireNonNull( standardServiceComponents );
		Objects.requireNonNull( sessionFactoryReference );
		Objects.requireNonNull( beanValidationPlan );
		Objects.requireNonNull( integratorLifecycle );
	}
}
