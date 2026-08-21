/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.relational.internal;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Table;
import org.hibernate.relational.SchemaManager;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaFilterProvider;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.SchemaValidationException;

import static org.hibernate.cfg.MappingSettings.DEFAULT_CATALOG;
import static org.hibernate.cfg.MappingSettings.DEFAULT_SCHEMA;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_IMPORT_FILES;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_LOAD_SCRIPT_SOURCE;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_SKIP_DEFAULT_IMPORT_FILE;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_DATABASE_ACTION;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_FILTER_PROVIDER;

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
		return new SchemaManagerImpl( sessionFactory, metadata, schemaName, catalogName );
	}

	@Override
	public SchemaManager forCatalog(String catalogName) {
		return new SchemaManagerImpl( sessionFactory, metadata, schemaName, catalogName );
	}

	private void addSchemaAndCatalog(Map<String, Object> properties) {
		if ( schemaName != null ) {
			properties.put( DEFAULT_SCHEMA, schemaName );
		}
		if ( catalogName != null ) {
			properties.put( DEFAULT_CATALOG, catalogName );
		}
	}

	@Override
	public void exportMappedObjects(boolean createSchemas) {
		Map<String, Object> properties = new HashMap<>( sessionFactory.getProperties() );
		properties.put( JAKARTA_HBM2DDL_DATABASE_ACTION, Action.CREATE_ONLY );
		properties.put( JAKARTA_HBM2DDL_SCRIPTS_ACTION, Action.NONE );
		properties.put( JAKARTA_HBM2DDL_CREATE_SCHEMAS, createSchemas );
		addSchemaAndCatalog( properties );
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
		properties.put( JAKARTA_HBM2DDL_DATABASE_ACTION, Action.DROP );
		properties.put( JAKARTA_HBM2DDL_SCRIPTS_ACTION, Action.NONE );
		properties.put( JAKARTA_HBM2DDL_CREATE_SCHEMAS, dropSchemas );
		addSchemaAndCatalog( properties );
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
		properties.put( JAKARTA_HBM2DDL_DATABASE_ACTION, Action.VALIDATE );
		properties.put( JAKARTA_HBM2DDL_SCRIPTS_ACTION, Action.NONE );
		properties.put( JAKARTA_HBM2DDL_CREATE_SCHEMAS, false );
		addSchemaAndCatalog( properties );
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
		properties.put( JAKARTA_HBM2DDL_DATABASE_ACTION, Action.TRUNCATE );
		properties.put( JAKARTA_HBM2DDL_SCRIPTS_ACTION, Action.NONE );
		addSchemaAndCatalog( properties );
		SchemaManagementToolCoordinator.process(
				metadata,
				sessionFactory.getServiceRegistry(),
				properties,
				action -> {}
		);
	}

	@Override
	public void truncateTable(String tableName) {
		Map<String, Object> properties = new HashMap<>( sessionFactory.getProperties() );
		properties.put( JAKARTA_HBM2DDL_DATABASE_ACTION, Action.TRUNCATE );
		properties.put( JAKARTA_HBM2DDL_SCRIPTS_ACTION, Action.NONE );
		properties.put( HBM2DDL_SKIP_DEFAULT_IMPORT_FILE, true );
		properties.remove( JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE );
		properties.remove( HBM2DDL_LOAD_SCRIPT_SOURCE );
		properties.remove( HBM2DDL_IMPORT_FILES );
		properties.put( HBM2DDL_FILTER_PROVIDER, new SchemaFilterProvider() {
			@Override
			public SchemaFilter getCreateFilter() {
				throw new UnsupportedOperationException();
			}

			@Override
			public SchemaFilter getDropFilter() {
				throw new UnsupportedOperationException();
			}

			@Override
			public SchemaFilter getTruncatorFilter() {
				return new SchemaFilter() {
					@Override
					public boolean includeNamespace(Namespace namespace) {
						return true;
					}

					@Override
					public boolean includeTable(Table table) {
						return table.getName().equals( tableName );
					}

					@Override
					public boolean includeSequence(Sequence sequence) {
						return false;
					}
				};
			}

			@Override
			public SchemaFilter getMigrateFilter() {
				throw new UnsupportedOperationException();
			}

			@Override
			public SchemaFilter getValidateFilter() {
				throw new UnsupportedOperationException();
			}
		} );
		addSchemaAndCatalog( properties );
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
		properties.put( JAKARTA_HBM2DDL_DATABASE_ACTION, Action.POPULATE );
		properties.put( JAKARTA_HBM2DDL_SCRIPTS_ACTION, Action.NONE );
		addSchemaAndCatalog( properties );
		SchemaManagementToolCoordinator.process(
				metadata,
				sessionFactory.getServiceRegistry(),
				properties,
				action -> {}
		);
	}

	@Override
	public void resynchronizeGenerators() {
		Map<String, Object> properties = new HashMap<>( sessionFactory.getProperties() );
		properties.put( JAKARTA_HBM2DDL_DATABASE_ACTION, Action.SYNCHRONIZE );
		properties.put( JAKARTA_HBM2DDL_SCRIPTS_ACTION, Action.NONE );
		addSchemaAndCatalog( properties );
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
