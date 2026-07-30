/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import org.hibernate.Incubating;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.scan.spi.Scanner;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.jpa.spi.MutableJpaCompliance;
import org.hibernate.metamodel.spi.ManagedTypeRepresentationResolver;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Defines the shared environment used while resolving mapping metadata.
 * <p>
 * The context owns both durable bootstrap products, such as the
 * {@link TypeConfiguration} and {@link ModelsContext}, and temporary
 * source-processing resources, such as the Jakarta Persistence temporary
 * class loader and scanning state. {@link #release()} releases only the
 * temporary resources when metadata materialization completes. Durable
 * products may remain available to the subsequent
 * {@link org.hibernate.SessionFactory} construction step.
 * <p>
 * This broad context is retained for metadata-building compatibility.
 * New bootstrap and factory-construction SPIs should expose focused resolved
 * products instead of exposing {@code BootstrapContext}.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface BootstrapContext {
	/**
	 * The service registry available to bootstrapping
	 */
	StandardServiceRegistry getServiceRegistry();

	/**
	 * In-flight form of {@link org.hibernate.jpa.spi.JpaCompliance}
	 */
	MutableJpaCompliance getJpaCompliance();

	/**
	 * The {@link TypeConfiguration} belonging to this {@code BootstrapContext}.
	 *
	 * @see TypeConfiguration
	 */
	TypeConfiguration getTypeConfiguration();

	/**
	 * Access to the {@code hibernate-models} {@linkplain ModelsContext}
	 */
	@Incubating
	ModelsContext getModelsContext();

	/**
	 * The {@link BeanInstanceProducer} to use when creating custom type references.
	 *
	 * @implNote Usually a {@link org.hibernate.boot.internal.TypeBeanInstanceProducer}.
	 */
	BeanInstanceProducer getCustomTypeProducer();

	/**
	 * Access to the {@link ClassLoaderService}.
	 */
	ClassLoaderService getClassLoaderService();

	/**
	 * Access to the {@link ManagedBeanRegistry}.
	 */
	ManagedBeanRegistry getManagedBeanRegistry();

	/**
	 * Access to the {@link ConfigurationService}.
	 */
	ConfigurationService getConfigurationService();

	/**
	 * Whether the bootstrap was initiated from JPA bootstrapping.
	 *
	 * @implSpec This is used
	 *
	 * @see #markAsJpaBootstrap()
	 */
	boolean isJpaBootstrap();

	/**
	 * Indicates that bootstrap was initiated from JPA bootstrapping.
	 *
	 * @implSpec Internally, {@code false} is the assumed value.
	 *           We only need to call this to mark it {@code true}.
	 */
	void markAsJpaBootstrap();

	/**
	 * Access the temporary {@link ClassLoader} passed to us, as defined by
	 * {@link jakarta.persistence.spi.PersistenceUnitInfo#getNewTempClassLoader()},
	 * if any.
	 *
	 * @return The temporary {@code ClassLoader}
	 */
	ClassLoader getJpaTempClassLoader();

	/**
	 * Access to class loading capabilities.
	 */
	ClassLoaderAccess getClassLoaderAccess();

	/**
	 * Access to the {@link ArchiveDescriptorFactory} used for scanning.
	 *
	 * @return The {@link ArchiveDescriptorFactory}
	 */
	ArchiveDescriptorFactory getArchiveDescriptorFactory();

	/**
	 * Access to the {@link Scanner} to be used
	 * for scanning.
	 * <p>
	 * Can be:
	 * <ul>
	 *     <li>An instance of {@link Scanner},
	 *     <li>a {@code Class} reference to the {@code Scanner} implementor, or
	 *     <li>a string naming the {@code Scanner} implementor.
	 * </ul>
	 *
	 * @return The scanner
	 */
	Object getScanning();

	/**
	 * @see ManagedTypeRepresentationResolver
	 */
	ManagedTypeRepresentationResolver getRepresentationStrategySelector();

	/**
	 * Releases temporary source-processing resources held by this context.
	 * <p>
	 * This occurs when metadata materialization completes. It does not invalidate
	 * durable products such as the {@link TypeConfiguration} or
	 * {@link ModelsContext}, which may be used during factory construction.
	 */
	void release();
}
