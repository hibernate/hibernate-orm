/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.spi;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.models.categorize.spi.GlobalRegistrations;
import org.hibernate.models.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.service.ServiceRegistry;

import jakarta.persistence.SharedCacheMode;

/// Binding-time access to bootstrap services and shared categorization results.
///
/// The context adapts the bootstrap model infrastructure for binders.  It exposes
/// naming strategies, cache mode, Hibernate Models registries, services, and
/// persistence-unit scoped registrations already collected during categorization.
/// Binding code should use this contract for services and use {@link BindingState}
/// for mutable binding results.
///
/// @since 9.0
/// @author Steve Ebersole
public interface BindingContext {

	/// Persistence-unit scoped registrations collected during categorization.
	GlobalRegistrations getGlobalRegistrations();

	/// Registry used to resolve model class descriptors while binding.
	default ClassDetailsRegistry getClassDetailsRegistry() {
		return getBootstrapContext().getModelsContext().getClassDetailsRegistry();
	}

	/// Shared cache mode in effect for this binding run.
	SharedCacheMode getSharedCacheMode();

	/// Implicit naming strategy used when logical names must be inferred.
	ImplicitNamingStrategy getImplicitNamingStrategy();

	/// Physical naming strategy used to translate logical names to physical names.
	PhysicalNamingStrategy getPhysicalNamingStrategy();

	/// Bootstrap context for services shared across boot phases.
	BootstrapContext getBootstrapContext();

	/// Service registry from the bootstrap context.
	default ServiceRegistry getServiceRegistry() {
		return getBootstrapContext().getServiceRegistry();
	}

	/// Registry used to resolve annotation descriptors while binding.
	default AnnotationDescriptorRegistry getAnnotationDescriptorRegistry() {
		return getBootstrapContext().getModelsContext().getAnnotationDescriptorRegistry();
	}
}
