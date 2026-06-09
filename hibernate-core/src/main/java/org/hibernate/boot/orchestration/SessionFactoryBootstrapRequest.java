/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.orchestration;

import java.util.Objects;

import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.models.source.BootstrapSourceContributions;
import org.hibernate.boot.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.settings.ResolvedSessionFactorySettings;
import org.hibernate.service.ServiceRegistry;

/// Resolved inputs for one-shot SessionFactory bootstrap.
///
/// @since 9.0
/// @author Steve Ebersole
public record SessionFactoryBootstrapRequest(
		ResolvedBootstrapSettings bootstrapSettings,
		BootstrapSourceContributions sourceContributions,
		ResolvedSessionFactorySettings sessionFactorySettings,
		ServiceRegistry serviceRegistry,
		SessionFactoryObserver[] additionalSessionFactoryObservers) {

	public SessionFactoryBootstrapRequest {
		Objects.requireNonNull( bootstrapSettings );
		Objects.requireNonNull( sourceContributions );
		Objects.requireNonNull( sessionFactorySettings );
		Objects.requireNonNull( serviceRegistry );
		additionalSessionFactoryObservers = additionalSessionFactoryObservers == null
				? new SessionFactoryObserver[0]
				: additionalSessionFactoryObservers.clone();
	}

	@Override
	public SessionFactoryObserver[] additionalSessionFactoryObservers() {
		return additionalSessionFactoryObservers.clone();
	}
}
