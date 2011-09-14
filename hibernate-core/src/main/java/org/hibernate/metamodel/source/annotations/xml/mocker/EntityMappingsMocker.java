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
import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.Index;
import org.jboss.logging.Logger;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.jaxb.mapping.orm.JaxbAccessType;
import org.hibernate.internal.jaxb.mapping.orm.JaxbEmbeddable;
import org.hibernate.internal.jaxb.mapping.orm.JaxbEntity;
import org.hibernate.internal.jaxb.mapping.orm.JaxbEntityMappings;
import org.hibernate.internal.jaxb.mapping.orm.JaxbMappedSuperclass;
import org.hibernate.internal.jaxb.mapping.orm.JaxbPersistenceUnitDefaults;
import org.hibernate.internal.jaxb.mapping.orm.JaxbPersistenceUnitMetadata;
import org.hibernate.service.ServiceRegistry;

/**
 * Parse all {@link org.hibernate.internal.jaxb.mapping.orm.JaxbEntityMappings} generated from orm.xml.
 *
 * @author Strong Liu
 */
public class EntityMappingsMocker {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			EntityMappingsMocker.class.getName()
	);
	private final List<JaxbEntityMappings> entityMappingsList;
	/**
	 * Default configuration defined in Persistence Metadata Unit, one or zero per Persistence Unit.
	 */
	private Default globalDefaults;
	private final IndexBuilder indexBuilder;
	private final GlobalAnnotations globalAnnotations;

	public EntityMappingsMocker(List<JaxbEntityMappings> entityMappingsList, Index index, ServiceRegistry serviceRegistry) {
		this.entityMappingsList = entityMappingsList;
		this.indexBuilder = new IndexBuilder( index, serviceRegistry );
		this.globalAnnotations = new GlobalAnnotations();
	}

	/**
	 * Create new {@link Index} with mocking JPA annotations from {@link org.hibernate.internal.jaxb.mapping.orm.JaxbEntityMappings} and merge them with existing {@link Index}
	 *
	 * @return new {@link Index}
	 */
	public Index mockNewIndex() {
		processPersistenceUnitMetadata( entityMappingsList );
		processEntityMappings( entityMappingsList );
		processGlobalAnnotations();
		return indexBuilder.build( globalDefaults );
	}

	/**
	 * processing PersistenceUnitMetadata, there should be only one PersistenceUnitMetadata in all mapping xml files.
	 */
	private void processPersistenceUnitMetadata(List<JaxbEntityMappings> entityMappingsList) {
		for ( JaxbEntityMappings entityMappings : entityMappingsList ) {
			//we have to iterate entityMappingsList first to find persistence-unit-metadata
			JaxbPersistenceUnitMetadata pum = entityMappings.getPersistenceUnitMetadata();
			if ( globalDefaults != null ) {
				LOG.duplicateMetadata();
				return;
			}
			if ( pum == null ) {
				continue;
			}
			globalDefaults = new Default();
			if ( pum.getXmlMappingMetadataComplete() != null ) {
				globalDefaults.setMetadataComplete( true );
				indexBuilder.mappingMetadataComplete();
			}
			JaxbPersistenceUnitDefaults pud = pum.getPersistenceUnitDefaults();
			if ( pud == null ) {
				return;
			}
			globalDefaults.setSchema( pud.getSchema() );
			globalDefaults.setCatalog( pud.getCatalog() );
			//globalDefaults.setAccess( pud.getAccess() );
			globalDefaults.setCascadePersist( pud.getCascadePersist() != null );
			new PersistenceMetadataMocker( indexBuilder, pud ).process();
		}
	}


	private void processEntityMappings(List<JaxbEntityMappings> entityMappingsList) {
		List<AbstractEntityObjectMocker> mockerList = new ArrayList<AbstractEntityObjectMocker>();
		for ( JaxbEntityMappings entityMappings : entityMappingsList ) {
			final Default defaults = getEntityMappingsDefaults( entityMappings );
			globalAnnotations.collectGlobalMappings( entityMappings, defaults );
			for ( JaxbMappedSuperclass mappedSuperclass : entityMappings.getMappedSuperclass() ) {
				AbstractEntityObjectMocker mocker =
						new MappedSuperclassMocker( indexBuilder, mappedSuperclass, defaults );
				mockerList.add( mocker );
				mocker.preProcess();
			}
			for ( JaxbEmbeddable embeddable : entityMappings.getEmbeddable() ) {
				AbstractEntityObjectMocker mocker =
						new EmbeddableMocker( indexBuilder, embeddable, defaults );
				mockerList.add( mocker );
				mocker.preProcess();
			}
			for ( JaxbEntity entity : entityMappings.getEntity() ) {
				globalAnnotations.collectGlobalMappings( entity, defaults );
				AbstractEntityObjectMocker mocker =
						new EntityMocker( indexBuilder, entity, defaults );
				mockerList.add( mocker );
				mocker.preProcess();
			}
		}
		for ( AbstractEntityObjectMocker mocker : mockerList ) {
			mocker.process();
		}
	}

	private void processGlobalAnnotations() {
		if ( globalAnnotations.hasGlobalConfiguration() ) {
			indexBuilder.collectGlobalConfigurationFromIndex( globalAnnotations );
			new GlobalAnnotationMocker(
					indexBuilder, globalAnnotations
			).process();
		}
	}

	private Default getEntityMappingsDefaults(JaxbEntityMappings entityMappings) {
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
		private JaxbAccessType access;
		private String packageName;
		private String schema;
		private String catalog;
		private Boolean metadataComplete;
		private Boolean cascadePersist;

		public JaxbAccessType getAccess() {
			return access;
		}

		void setAccess(JaxbAccessType access) {
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

		public Boolean isMetadataComplete() {
			return metadataComplete;
		}

		void setMetadataComplete(Boolean metadataComplete) {
			this.metadataComplete = metadataComplete;
		}

		public Boolean isCascadePersist() {
			return cascadePersist;
		}

		void setCascadePersist(Boolean cascadePersist) {
			this.cascadePersist = cascadePersist;
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
				if ( globalDefault.isCascadePersist() != null ) {
					cascadePersist = globalDefault.isCascadePersist();
				}
				if ( globalDefault.isMetadataComplete() != null ) {
					metadataComplete = globalDefault.isMetadataComplete();
				}

			}
		}
	}
}
