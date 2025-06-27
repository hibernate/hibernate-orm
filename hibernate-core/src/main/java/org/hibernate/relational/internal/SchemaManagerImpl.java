/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.relational.internal;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.relational.SchemaManager;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.SchemaValidationException;

/**
 * Implementation of {@link SchemaManager}, backed by a {@link SessionFactoryImplementor}
 * and {@link SchemaManagementToolCoordinator}.
 *
 * @author Gavin King
 */
public class SchemaManagerImpl implements SchemaManager {
	private final SessionFactoryImplementor sessionFactory;
	private final MetadataImplementor metadata;
	private final String schemaName;
	private final String catalogName;

	public SchemaManagerImpl(
			SessionFactoryImplementor sessionFactory,
			MetadataImplementor metadata) {
		this( sessionFactory, metadata, null, null );
	}

	public SchemaManagerImpl(
			SessionFactoryImplementor sessionFactory,
			MetadataImplementor metadata,
			String schemaName, String catalogName) {
		this.sessionFactory = sessionFactory;
		this.metadata = metadata;
		this.schemaName = schemaName;
		this.catalogName = catalogName;
	}

	@Override
	public SchemaManager forSchema(String schemaName) {
		return new SchemaManagerImpl( sessionFactory, metadata, schemaName, null );
	}

	@Override
	public SchemaManager forSchemaAndCatalog(String schemaName, String catalogName) {
		return new SchemaManagerImpl( sessionFactory, metadata, schemaName, catalogName );
	}

	@Override
	public void exportMappedObjects(boolean createSchemas) {
		Map<String, Object> properties = new HashMap<>( sessionFactory.getProperties() );
		properties.put( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, Action.CREATE_ONLY );
		properties.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, Action.NONE );
		properties.put( AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS, createSchemas );
		properties.put( AvailableSettings.DEFAULT_SCHEMA, schemaName );
		properties.put( AvailableSettings.DEFAULT_CATALOG, catalogName );
		SchemaManagementToolCoordinator.process(
				metadata,
				sessionFactory.getServiceRegistry(),
				properties,
				action -> {}
		);
	}

	@Override
	public void dropMappedObjects(boolean dropSchemas) {
		Map<String, Object> properties = new HashMap<>( sessionFactory.getProperties() );
		properties.put( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, Action.DROP );
		properties.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, Action.NONE );
		properties.put( AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS, dropSchemas );
		properties.put( AvailableSettings.DEFAULT_SCHEMA, schemaName );
		properties.put( AvailableSettings.DEFAULT_CATALOG, catalogName );
		SchemaManagementToolCoordinator.process(
				metadata,
				sessionFactory.getServiceRegistry(),
				properties,
				action -> {}
		);
	}

	@Override
	public void validateMappedObjects() {
		Map<String, Object> properties = new HashMap<>( sessionFactory.getProperties() );
		properties.put( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, Action.VALIDATE );
		properties.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, Action.NONE );
		properties.put( AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS, false );
		properties.put( AvailableSettings.DEFAULT_SCHEMA, schemaName );
		properties.put( AvailableSettings.DEFAULT_CATALOG, catalogName );
		SchemaManagementToolCoordinator.process(
				metadata,
				sessionFactory.getServiceRegistry(),
				properties,
				action -> {}
		);
	}

	@Override
	public void truncateMappedObjects() {
		Map<String, Object> properties = new HashMap<>( sessionFactory.getProperties() );
		properties.put( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, Action.TRUNCATE );
		properties.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, Action.NONE );
		properties.put( AvailableSettings.DEFAULT_SCHEMA, schemaName );
		properties.put( AvailableSettings.DEFAULT_CATALOG, catalogName );
		SchemaManagementToolCoordinator.process(
				metadata,
				sessionFactory.getServiceRegistry(),
				properties,
				action -> {}
		);
	}

	@Override
	public void populate() {
		Map<String, Object> properties = new HashMap<>( sessionFactory.getProperties() );
		properties.put( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, Action.POPULATE );
		properties.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, Action.NONE );
		properties.put( AvailableSettings.DEFAULT_SCHEMA, schemaName );
		properties.put( AvailableSettings.DEFAULT_CATALOG, catalogName );
		SchemaManagementToolCoordinator.process(
				metadata,
				sessionFactory.getServiceRegistry(),
				properties,
				action -> {}
		);
	}

	@Override
	public void create(boolean createSchemas) {
		exportMappedObjects( createSchemas );
	}

	@Override
	public void drop(boolean dropSchemas) {
		dropMappedObjects( dropSchemas );
	}

	@Override
	public void validate() throws SchemaValidationException {
		try {
			validateMappedObjects();
		}
		catch ( SchemaManagementException sme ) {
			throw new SchemaValidationException( sme.getMessage(), sme );
		}
	}

	@Override
	public void truncate() {
		truncateMappedObjects();
	}
}
