/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.temporal.spi.ChangesetCoordinator;
import org.hibernate.type.spi.TypeConfiguration;

/// Services used while resolving mapping details.
///
/// @since 9.0
/// @author Steve Ebersole
public interface MappingResolutionServices {
	ServiceRegistry getServiceRegistry();

	StandardServiceRegistry getStandardServiceRegistry();

	ConfigurationService getConfigurationService();

	ClassLoaderService getClassLoaderService();

	ClassLoaderAccess getClassLoaderAccess();

	ManagedBeanRegistry getManagedBeanRegistry();

	BeanInstanceProducer getCustomTypeProducer();

	ModelsContext getModelsContext();

	TypeConfiguration getTypeConfiguration();

	JdbcServices getJdbcServices();

	ChangesetCoordinator getChangesetCoordinator();

	StrategySelector getStrategySelector();
}
