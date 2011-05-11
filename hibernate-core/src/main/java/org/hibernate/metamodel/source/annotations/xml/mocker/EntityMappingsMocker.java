/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc..
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.source.annotations.xml.mocker;

import java.io.Serializable;
import java.util.List;

import org.jboss.jandex.Index;
import org.jboss.logging.Logger;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.source.annotation.xml.XMLAccessType;
import org.hibernate.metamodel.source.annotation.xml.XMLEmbeddable;
import org.hibernate.metamodel.source.annotation.xml.XMLEntity;
import org.hibernate.metamodel.source.annotation.xml.XMLEntityListeners;
import org.hibernate.metamodel.source.annotation.xml.XMLEntityMappings;
import org.hibernate.metamodel.source.annotation.xml.XMLMappedSuperclass;
import org.hibernate.metamodel.source.annotation.xml.XMLPersistenceUnitDefaults;
import org.hibernate.metamodel.source.annotation.xml.XMLPersistenceUnitMetadata;
import org.hibernate.service.ServiceRegistry;

/**
 * Parse all {@link XMLEntityMappings} generated from orm.xml.
 *
 * @author Strong Liu
 */
public class EntityMappingsMocker {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			EntityMappingsMocker.class.getName()
	);
	private List<XMLEntityMappings> entityMappingsList;
	//todo delimited-identifier
	private Default globalDefaults;
	final private IndexBuilder indexBuilder;
	//todo
	private XMLEntityListeners defaultEntityListeners;
	final private GlobalAnnotations globalAnnotations;

	public EntityMappingsMocker(List<XMLEntityMappings> entityMappingsList, Index index, ServiceRegistry serviceRegistry) {
		this.entityMappingsList = entityMappingsList;
		this.indexBuilder = new IndexBuilder( index, serviceRegistry );
		this.globalAnnotations = new GlobalAnnotations();
	}

	/**
	 * Create new {@link Index} with mocking JPA annotations from {@link XMLEntityMappings} and merge them with existing {@link Index}
	 *
	 * @return new {@link Index}
	 */
	public Index mockNewIndex() {
		processPersistenceUnitMetadata( entityMappingsList );
		processEntityMappings( entityMappingsList );
		processGlobalConfiguration();
		return indexBuilder.build( globalDefaults );
	}

	/**
	 * processing PersistenceUnitMetadata, there should be only one PersistenceUnitMetadata in all mapping xml files.
	 */
	private void processPersistenceUnitMetadata(List<XMLEntityMappings> entityMappingsList) {
		for ( XMLEntityMappings entityMappings : entityMappingsList ) {
			//we have to iterate entityMappingsList first to find persistence-unit-metadata
			XMLPersistenceUnitMetadata pum = entityMappings.getPersistenceUnitMetadata();
			if ( pum == null ) {
				continue;
			}
			if ( globalDefaults == null ) {
				globalDefaults = new Default();
				globalDefaults.setMetadataComplete( pum.getXmlMappingMetadataComplete() != null );
				indexBuilder.mappingMetadataComplete( globalDefaults );
				XMLPersistenceUnitDefaults pud = pum.getPersistenceUnitDefaults();
				if ( pud == null ) {
					return;
				}
				globalDefaults.setSchema( pud.getSchema() );
				globalDefaults.setCatalog( pud.getCatalog() );
				globalDefaults.setAccess( pud.getAccess() );
				globalDefaults.setCascadePersist( pud.getCascadePersist() != null );
				globalDefaults.setDelimitedIdentifiers( pud.getDelimitedIdentifiers() != null );
				defaultEntityListeners = pud.getEntityListeners();
			}
			else {
				LOG.duplicateMetadata();
			}
		}
	}


	private void processEntityMappings(List<XMLEntityMappings> entityMappingsList) {
		for ( XMLEntityMappings entityMappings : entityMappingsList ) {
			final Default defaults = getEntityMappingsDefaults( entityMappings );
			globalAnnotations.collectGlobalMappings( entityMappings, defaults );
			for ( XMLMappedSuperclass mappedSuperclass : entityMappings.getMappedSuperclass() ) {
				new MappedSuperclassMocker( indexBuilder, mappedSuperclass, defaults ).process();
			}
			for ( XMLEmbeddable embeddable : entityMappings.getEmbeddable() ) {
				new EmbeddableMocker( indexBuilder, embeddable, defaults ).process();
			}
			for ( XMLEntity entity : entityMappings.getEntity() ) {
				globalAnnotations.collectGlobalMappings( entity, defaults );
				new EntityMocker( indexBuilder, entity, defaults ).process();
			}
		}
	}

	private void processGlobalConfiguration() {
		if ( globalAnnotations.hasGlobalConfiguration() ) {
			indexBuilder.collectGlobalConfigurationFromIndex( globalAnnotations );
			new GlobalConfigurationMocker(
					indexBuilder, globalAnnotations
			).parser();
		}
	}

	private Default getEntityMappingsDefaults(XMLEntityMappings entityMappings) {
		Default entityMappingDefault = new Default();
		entityMappingDefault.setPackageName( entityMappings.getPackage() );
		entityMappingDefault.setSchema( entityMappings.getSchema() );
		entityMappingDefault.setCatalog( entityMappings.getCatalog() );
		entityMappingDefault.setAccess( entityMappings.getAccess() );
		final Default defaults = new Default();
		defaults.override( globalDefaults );
		defaults.override( entityMappingDefault );
		return defaults;
	}


	public static class Default implements Serializable {
		private XMLAccessType access;
		private String packageName;
		private String schema;
		private String catalog;
		private Boolean metadataComplete;
		private Boolean cascadePersist;
		private Boolean delimitedIdentifier;

		public XMLAccessType getAccess() {
			return access;
		}

		void setAccess(XMLAccessType access) {
			this.access = access;
		}

		public String getCatalog() {
			return catalog;
		}

		void setCatalog(String catalog) {
			this.catalog = catalog;
		}

		public String getPackageName() {
			return packageName;
		}

		void setPackageName(String packageName) {
			this.packageName = packageName;
		}

		public String getSchema() {
			return schema;
		}

		void setSchema(String schema) {
			this.schema = schema;
		}

		public Boolean getMetadataComplete() {
			return metadataComplete;
		}

		void setMetadataComplete(Boolean metadataComplete) {
			this.metadataComplete = metadataComplete;
		}

		public Boolean getCascadePersist() {
			return cascadePersist;
		}

		void setCascadePersist(Boolean cascadePersist) {
			this.cascadePersist = cascadePersist;
		}

		void setDelimitedIdentifiers(Boolean delimitedIdentifier) {
			this.delimitedIdentifier = delimitedIdentifier;
		}

		public Boolean getDelimitedIdentifier() {
			return delimitedIdentifier;
		}

		void override(Default globalDefault) {
			if ( globalDefault != null ) {
				if ( globalDefault.getAccess() != null ) {
					access = globalDefault.getAccess();
				}
				if ( globalDefault.getPackageName() != null ) {
					packageName = globalDefault.getPackageName();
				}
				if ( globalDefault.getSchema() != null ) {
					schema = globalDefault.getSchema();
				}
				if ( globalDefault.getCatalog() != null ) {
					catalog = globalDefault.getCatalog();
				}
				if ( globalDefault.getDelimitedIdentifier() != null ) {
					delimitedIdentifier = globalDefault.getDelimitedIdentifier();
				}
				if ( globalDefault.getMetadataComplete() != null ) {
					metadataComplete = globalDefault.getMetadataComplete();
				}
				if ( globalDefault.getCascadePersist() != null ) {
					cascadePersist = globalDefault.getCascadePersist();
				}
			}
		}


	}


}
