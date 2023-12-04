/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.spi;

import org.hibernate.boot.internal.ClassmateContext;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.models.categorize.spi.GlobalRegistrations;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.models.spi.SourceModelContext;
import org.hibernate.service.ServiceRegistry;

import jakarta.persistence.SharedCacheMode;

/**
 * Contextual information used while {@linkplain BindingCoordinator table}
 * {@linkplain org.hibernate.boot.model.process.spi.ManagedResources managed-resources} into
 * into Hibernate's {@linkplain org.hibernate.mapping boot-time model}.
 *
 * @author Steve Ebersole
 */
public interface BindingContext extends SourceModelContext {

	GlobalRegistrations getGlobalRegistrations();

	ClassmateContext getClassmateContext();

	SharedCacheMode getSharedCacheMode();

	ImplicitNamingStrategy getImplicitNamingStrategy();
	PhysicalNamingStrategy getPhysicalNamingStrategy();

	BootstrapContext getBootstrapContext();

	default ServiceRegistry getServiceRegistry() {
		return getBootstrapContext().getServiceRegistry();
	}
}
