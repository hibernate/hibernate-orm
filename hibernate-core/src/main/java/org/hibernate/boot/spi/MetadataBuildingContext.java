/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import org.hibernate.audit.AuditStrategy;
import org.hibernate.boot.model.TypeDefinitionRegistry;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.pipeline.internal.MappingResolutionOptions;
import org.hibernate.boot.mapping.internal.context.MappingResolutionServices;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.temporal.TemporalTableStrategy;
import org.hibernate.temporal.spi.ChangesetCoordinator;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.audit.AuditStrategy.DEFAULT;
import static org.hibernate.temporal.TemporalTableStrategy.AUTO;

/**
 * Describes the context in which the process of building {@link org.hibernate.boot.Metadata}.
 * <p>
 * {@link MetadataBuildingContext}s are hierarchical: global, persistence unit, document, mapping.
 *
 * @author Steve Ebersole
 *
 * @since 5.0
 */
public interface MetadataBuildingContext {
	BootstrapContext getBootstrapContext();

	MappingResolutionServices getServiceComponents();

	/**
	 * Access to the metadata building plan.
	 *
	 * @return The plan
	 */
	MappingResolutionOptions getBuildingPlan();

	/**
	 * Access to mapping defaults in effect for this context
	 *
	 * @return The mapping defaults.
	 */
	EffectiveMappingDefaults getEffectiveDefaults();

	/**
	 * Access to the collector of metadata as we build it.
	 *
	 * @return The metadata collector.
	 */
	InFlightMetadataCollector getMetadataCollector();

	/**
	 * Not sure how I feel about this exposed here
	 *
	 * @return The ObjectNameNormalizer
	 */
	ObjectNameNormalizer getObjectNameNormalizer();

	TypeDefinitionRegistry getTypeDefinitionRegistry();

	/**
	 * The name of the contributor whose mappings we are currently processing
	 */
	String getCurrentContributorName();

	default TemporalTableStrategy getTemporalTableStrategy() {
		return AUTO;
	}

	default AuditStrategy getAuditStrategy() {
		return DEFAULT;
	}

	default JpaCompliance getJpaCompliance() {
		return getBootstrapContext().getJpaCompliance();
	}

	default void registerAdHocBasicType(BasicType<?> basicType) {
	}

	default <T> BasicType<T> resolveAdHocBasicType(String key) {
		return null;
	}

	default <T> BasicType<T> findAdHocBasicType(JavaType<T> javaType, JdbcType jdbcType) {
		return null;
	}

	default ServiceRegistry getServiceRegistry() {
		return getServiceComponents().getServiceRegistry();
	}

	default StandardServiceRegistry getStandardServiceRegistry() {
		return getServiceComponents().getStandardServiceRegistry();
	}

	default ConfigurationService getConfigurationService() {
		return getServiceComponents().getConfigurationService();
	}

	default ClassLoaderService getClassLoaderService() {
		return getServiceComponents().getClassLoaderService();
	}

	default ClassLoaderAccess getClassLoaderAccess() {
		return getServiceComponents().getClassLoaderAccess();
	}

	default ManagedBeanRegistry getManagedBeanRegistry() {
		return getServiceComponents().getManagedBeanRegistry();
	}

	default BeanInstanceProducer getCustomTypeProducer() {
		return getServiceComponents().getCustomTypeProducer();
	}

	default ModelsContext getModelsContext() {
		return getServiceComponents().getModelsContext();
	}

	default TypeConfiguration getTypeConfiguration() {
		return getServiceComponents().getTypeConfiguration();
	}

	default JdbcServices getJdbcServices() {
		return getServiceComponents().getJdbcServices();
	}

	default ChangesetCoordinator getChangesetCoordinator() {
		return getServiceComponents().getChangesetCoordinator();
	}
}
