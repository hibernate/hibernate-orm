/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.Incubating;
import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.metamodel.mapping.DiscriminatorType;
import org.hibernate.jpa.boot.spi.PersistenceUnitCallbackDefinition;
import org.hibernate.query.named.spi.NamedObjectRepository;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.spi.TypeConfiguration;


/**
 * The SPI-level {@link Metadata} contract.
 *
 * @author Steve Ebersole
 *
 * @since 5.0
 */
public interface MetadataImplementor extends Metadata {
	/**
	 * Access to the options used to build this {@code Metadata}
	 *
	 * @return The {@link MetadataBuildingOptions}
	 */
	MetadataBuildingOptions getMetadataBuildingOptions();

	/**
	 * Access to the {@link TypeConfiguration} belonging to the {@link BootstrapContext}
	 */
	TypeConfiguration getTypeConfiguration();

	/**
	 * Access to the {@link SqmFunctionRegistry} belonging to the {@link BootstrapContext}
	 */
	SqmFunctionRegistry getFunctionRegistry();

	/**
	 * Build the repository of named queries, named result set mappings,
	 * and named entity graphs.
	 */
	NamedObjectRepository buildNamedQueryRepository();

	/**
	 * Access to the {@link PersistenceUnitCallbackDefinition}s belonging to the {@link BootstrapContext}
	 *
	 * @since 8.0
	 */
	List<PersistenceUnitCallbackDefinition> getPersistenceUnitLifecycleCallbackDefinitions();

	@Incubating
	void orderColumns(boolean forceOrdering);

	void validate() throws MappingException;

	Set<MappedSuperclass> getMappedSuperclassMappingsCopy();

	void initSessionFactory(SessionFactoryImplementor sessionFactoryImplementor);

	void visitRegisteredComponents(Consumer<Component> consumer);

	Component getGenericComponent(Class<?> componentClass);

	DiscriminatorType<?> resolveEmbeddableDiscriminatorType(Class<?> embeddableClass, Supplier<DiscriminatorType<?>> supplier);

	@Override
	SessionFactoryImplementor buildSessionFactory();
}
