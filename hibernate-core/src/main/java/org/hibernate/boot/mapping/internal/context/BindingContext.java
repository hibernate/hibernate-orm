/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.context;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.mapping.internal.categorize.CategorizedDomainModel;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.pipeline.internal.MappingResolutionOptions;
import org.hibernate.boot.mapping.internal.categorize.GlobalRegistrations;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.models.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.spi.TypeConfiguration;

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

	CategorizedDomainModel getCategorizedDomainModel();

	/// Registry used to resolve model class descriptors while binding.
	default ClassDetailsRegistry getClassDetailsRegistry() {
		return getModelsContext().getClassDetailsRegistry();
	}

	/// Shared cache mode in effect for this binding run.
	SharedCacheMode getSharedCacheMode();

	/// Implicit naming strategy used when logical names must be inferred.
	ImplicitNamingStrategy getImplicitNamingStrategy();

	/// Physical naming strategy used to translate logical names to physical names.
	PhysicalNamingStrategy getPhysicalNamingStrategy();

	/// Service registry used while binding.
	ServiceRegistry getServiceRegistry();

	MappingResolutionOptions getBuildingPlan();

	ConfigurationService getConfigurationService();

	ClassLoaderService getClassLoaderService();

	BeanInstanceProducer getCustomTypeProducer();

	ManagedBeanRegistry getManagedBeanRegistry();

	ModelsContext getModelsContext();

	TypeConfiguration getTypeConfiguration();

	/// Registry used to resolve annotation descriptors while binding.
	default AnnotationDescriptorRegistry getAnnotationDescriptorRegistry() {
		return getModelsContext().getAnnotationDescriptorRegistry();
	}
}
