/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.orchestration;

import java.util.Objects;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/// One-shot coordinator for building a SessionFactory from resolved bootstrap inputs.
///
/// @see MetadataResolver
/// @see SessionFactoryBuilder
///
/// @since 9.0
/// @author Steve Ebersole
public class SessionFactoryBootstrap {
	public static SessionFactoryImplementor build(SessionFactoryBootstrapRequest request) {
		Objects.requireNonNull( request );
		final var resolvedMetadata = MetadataResolver.resolve(
				request.bootstrapSettings(),
				request.sourceContributions(),
				request.serviceRegistry()
		);
		return SessionFactoryBuilder.build(
				request.sessionFactorySettings(),
				resolvedMetadata,
				request.serviceRegistry(),
				request.additionalSessionFactoryObservers()
		);
	}
}
