/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import java.util.Collection;
import java.util.Map;

import org.hibernate.Incubating;
import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.internal.ClassmateContext;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.jpa.spi.MutableJpaCompliance;
import org.hibernate.metamodel.spi.ManagedTypeRepresentationResolver;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Defines a context for things available during the process of bootstrapping
 * a {@link org.hibernate.SessionFactory} which are expected to be cleaned up
 * after the {@code SessionFactory} is built.
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
	 * The {@link SqmFunctionRegistry} belonging to this {@code BootstrapContext}.
	 *
	 * @see SqmFunctionRegistry
	 */
	SqmFunctionRegistry getFunctionRegistry();

	/**
	 * The {@link BeanInstanceProducer} to use when creating custom type references.
	 *
	 * @implNote Usually a {@link org.hibernate.boot.model.TypeBeanInstanceProducer}.
	 */
	BeanInstanceProducer getCustomTypeProducer();

	/**
	 * Options specific to building the {@linkplain Metadata boot metamodel}
	 */
	MetadataBuildingOptions getMetadataBuildingOptions();

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
	 * Access to the shared {@link ClassmateContext} object used
	 * throughout the bootstrap process.
	 *
	 * @return Access to the shared {@link ClassmateContext} delegates.
	 */
	ClassmateContext getClassmateContext();

	/**
	 * Access to the {@link ArchiveDescriptorFactory} used for scanning.
	 *
	 * @return The {@link ArchiveDescriptorFactory}
	 */
	ArchiveDescriptorFactory getArchiveDescriptorFactory();

	/**
	 * Access to the options to be used for scanning.
	 *
	 * @return The scan options
	 */
	ScanOptions getScanOptions();

	/**
	 * Access to the environment for scanning.
	 *
	 * @apiNote Consider this temporary; see discussion on {@link ScanEnvironment}.
	 *
	 * @return The scan environment
	 */
	ScanEnvironment getScanEnvironment();

	/**
	 * Access to the {@link org.hibernate.boot.archive.scan.spi.Scanner} to be used
	 * for scanning.
	 * <p>
	 * Can be:
	 * <ul>
	 *     <li>An instance of {@link org.hibernate.boot.archive.scan.spi.Scanner},
	 *     <li>a {@code Class} reference to the {@code Scanner} implementor, or
	 *     <li>a string naming the {@code Scanner} implementor.
	 * </ul>
	 *
	 * @return The scanner
	 */
	Object getScanner();

	/**
	 * Access to the Jandex index passed by call to
	 * {@link org.hibernate.boot.MetadataBuilder#applyIndexView(Object)}, if any.
	 *
	 * @apiNote Jandex is currently not used, see
	 *          <a href="https://github.com/hibernate/hibernate-orm/wiki/Roadmap7.0">the roadmap</a>
	 *
	 * @return The Jandex index
	 */
	Object getJandexView();

	/**
	 * Access to any SQL functions explicitly registered with the
	 * {@link org.hibernate.boot.MetadataBuilder}.
	 * This does not include {@code Dialect}-registered functions.
	 * <p>
	 * Should never return {@code null}.
	 *
	 * @return The {@link SqmFunctionDescriptor}s registered via {@code MetadataBuilder}
	 */
	Map<String, SqmFunctionDescriptor> getSqlFunctions();

	/**
	 * Access to any {@link AuxiliaryDatabaseObject}s explicitly registered with
	 * the {@link org.hibernate.boot.MetadataBuilder}.
	 * This does not include {@link AuxiliaryDatabaseObject}s defined in mappings.
	 * <p>
	 * Should never return {@code null}.
	 *
	 * @return The {@link AuxiliaryDatabaseObject}s registered via {@code MetadataBuilder}
	 */
	Collection<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjectList();

	/**
	 * Access to collected {@link jakarta.persistence.AttributeConverter} definitions.
	 * <p>
	 * Should never return {@code null}.
	 *
	 * @return The {@link ConverterDescriptor}s registered via {@code MetadataBuilder}
	 */
	Collection<ConverterDescriptor> getAttributeConverters();

	/**
	 * Access to all explicit cache region mappings.
	 * <p>
	 * Should never return {@code null}.
	 *
	 * @return Explicit cache region mappings
	 */
	Collection<CacheRegionDefinition> getCacheRegionDefinitions();

	/**
	 * @see ManagedTypeRepresentationResolver
	 */
	ManagedTypeRepresentationResolver getRepresentationStrategySelector();

	/**
	 * Releases the "bootstrap only" resources held by this {@code BootstrapContext}.
	 */
	void release();

	/**
	 * To support Envers.
	 */
	void registerAdHocBasicType(BasicType<?> basicType);

	/**
	 * To support Envers.
	 */
	<T> BasicType<T> resolveAdHocBasicType(String key);
}
