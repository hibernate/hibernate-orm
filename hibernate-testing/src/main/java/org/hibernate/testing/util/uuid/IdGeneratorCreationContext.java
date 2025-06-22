/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.util.uuid;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Steve Ebersole
 */
public class IdGeneratorCreationContext implements GeneratorCreationContext {
	private final ServiceRegistry serviceRegistry;
	private final MetadataImplementor domainModel;
	private final RootClass entityMapping;

	public IdGeneratorCreationContext(
			ServiceRegistry serviceRegistry,
			MetadataImplementor domainModel,
			RootClass entityMapping) {
		this.serviceRegistry = serviceRegistry;
		this.domainModel = domainModel;
		this.entityMapping = entityMapping;

		assert entityMapping.getIdentifierProperty() != null;
	}

	public IdGeneratorCreationContext(MetadataImplementor domainModel, RootClass entityMapping) {
		this(
				domainModel.getMetadataBuildingOptions().getServiceRegistry(),
				domainModel,
				entityMapping
		);
	}

	@Override
	public RootClass getRootClass() {
		return entityMapping;
	}

	@Override
	public Database getDatabase() {
		return domainModel.getDatabase();
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public String getDefaultCatalog() {
		return "";
	}

	@Override
	public String getDefaultSchema() {
		return "";
	}

	@Override
	public PersistentClass getPersistentClass() {
		return entityMapping;
	}

	@Override
	public Property getProperty() {
		return entityMapping.getIdentifierProperty();
	}
}
