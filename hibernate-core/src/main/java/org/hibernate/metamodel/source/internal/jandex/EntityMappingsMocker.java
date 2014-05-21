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
package org.hibernate.metamodel.source.internal.jandex;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.AccessType;

import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEmbeddable;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntity;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntityMappings;
import org.hibernate.metamodel.source.internal.jaxb.JaxbMappedSuperclass;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPersistenceUnitDefaults;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPersistenceUnitMetadata;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.xml.spi.BindResult;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;

/**
 * Parse all {@link org.hibernate.metamodel.source.internal.jaxb.JaxbEntityMappings} generated from orm.xml.
 *
 * @author Strong Liu
 */
@Deprecated
public class EntityMappingsMocker {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( EntityMappingsMocker.class );

	private final List<BindResult<JaxbEntityMappings>> xmlBindings;

	/**
	 * Default configuration defined in Persistence Metadata Unit, one or zero per Persistence Unit.
	 */
	private Default globalDefaults;
	private final IndexBuilder indexBuilder;
	private final GlobalAnnotations globalAnnotations;

	public EntityMappingsMocker(List<BindResult<JaxbEntityMappings>> xmlBindings, IndexView index, ServiceRegistry serviceRegistry) {
		this.xmlBindings = xmlBindings;
		this.indexBuilder = new IndexBuilder( index, serviceRegistry );
		this.globalAnnotations = new GlobalAnnotations();
	}

	/**
	 * Create new {@link Index} with mocking JPA annotations from {@link org.hibernate.metamodel.source.internal.jaxb.JaxbEntityMappings}
	 * and merge them with existing {@link Index}
	 *
	 * @return new {@link Index}
	 */
	public Index mockNewIndex() {
		processPersistenceUnitMetadata( xmlBindings );
		processEntityMappings( xmlBindings );
		processGlobalAnnotations();
		return indexBuilder.build( globalDefaults );
	}

	/**
	 * processing PersistenceUnitMetadata, there should be only one PersistenceUnitMetadata in all mapping xml files.
	 */
	private void processPersistenceUnitMetadata(List<BindResult<JaxbEntityMappings>> xmlBindings) {
		for ( BindResult<JaxbEntityMappings> xmlBinding : xmlBindings ) {
			//we have to iterate entityMappingsList first to find persistence-unit-metadata
			JaxbPersistenceUnitMetadata pum = xmlBinding.getRoot().getPersistenceUnitMetadata();
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
			new PersistenceMetadataMocker( indexBuilder, pud, globalDefaults ).process();
		}
	}


	private void processEntityMappings(List<BindResult<JaxbEntityMappings>> xmlBindings) {
		List<AbstractEntityObjectMocker> mockerList = new ArrayList<AbstractEntityObjectMocker>();
		for ( BindResult<JaxbEntityMappings> xmlBinding : xmlBindings ) {
			final Default defaults = getEntityMappingsDefaults( xmlBinding.getRoot() );
			globalAnnotations.collectGlobalMappings( xmlBinding.getRoot(), defaults );
			for ( JaxbMappedSuperclass mappedSuperclass : xmlBinding.getRoot().getMappedSuperclass() ) {
				AbstractEntityObjectMocker mocker =
						new MappedSuperclassMocker( indexBuilder, mappedSuperclass, defaults );
				mockerList.add( mocker );
				mocker.preProcess();
			}
			for ( JaxbEmbeddable embeddable : xmlBinding.getRoot().getEmbeddable() ) {
				AbstractEntityObjectMocker mocker =
						new EmbeddableMocker( indexBuilder, embeddable, defaults );
				mockerList.add( mocker );
				mocker.preProcess();
			}
			for ( JaxbEntity entity : xmlBinding.getRoot().getEntity() ) {
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
			new GlobalAnnotationMocker( indexBuilder, globalAnnotations, globalDefaults	).process();
		}
	}

	private Default getEntityMappingsDefaults(JaxbEntityMappings entityMappings) {
		Default entityMappingDefault = new Default();
		entityMappingDefault.setPackageName( entityMappings.getPackage() );
		entityMappingDefault.setSchema( entityMappings.getSchema() );
		entityMappingDefault.setCatalog( entityMappings.getCatalog() );
		AccessType accessType = entityMappings.getAccess();
		if (accessType == null) {
			try {
				accessType = AccessType.valueOf( StringHelper.toUpperCase( entityMappings.getAttributeAccessor() ) );
			}
			catch (Exception e) {
				// ignore
			}
		}
		entityMappingDefault.setAccess( accessType );
		final Default defaults = new Default();
		defaults.override( globalDefaults );
		defaults.override( entityMappingDefault );
		return defaults;
	}
}
