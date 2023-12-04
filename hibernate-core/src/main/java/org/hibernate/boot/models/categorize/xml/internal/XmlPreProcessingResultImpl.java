/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.xml.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.models.categorize.xml.spi.XmlPreProcessingResult;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public class XmlPreProcessingResultImpl implements XmlPreProcessingResult {
	private final PersistenceUnitMetadataImpl persistenceUnitMetadata = new PersistenceUnitMetadataImpl();
	private final List<JaxbEntityMappingsImpl> documents = new ArrayList<>();
	private final List<String> managedClasses = new ArrayList<>();
	private final List<String> managedNames = new ArrayList<>();

	@Override
	public PersistenceUnitMetadataImpl getPersistenceUnitMetadata() {
		return persistenceUnitMetadata;
	}

	@Override
	public List<JaxbEntityMappingsImpl> getDocuments() {
		return documents;
	}

	@Override
	public List<String> getMappedClasses() {
		return managedClasses;
	}

	@Override
	public List<String> getMappedNames() {
		return managedNames;
	}

	public void addDocument(JaxbEntityMappingsImpl document) {
		persistenceUnitMetadata.apply( document.getPersistenceUnitMetadata() );
		documents.add( document );
		document.getEmbeddables().forEach( (jaxbEmbeddable) -> {
			if ( StringHelper.isNotEmpty( jaxbEmbeddable.getClazz() ) ) {
				managedClasses.add( XmlProcessingHelper.determineClassName( document, jaxbEmbeddable ) );
			}
			if ( StringHelper.isNotEmpty( jaxbEmbeddable.getName() ) ) {
				managedNames.add( jaxbEmbeddable.getName() );
			}
		} );
		document.getMappedSuperclasses().forEach( (jaxbMappedSuperclass) -> {
			managedClasses.add( XmlProcessingHelper.determineClassName( document, jaxbMappedSuperclass ) );
		} );
		document.getEntities().forEach( (jaxbEntity) -> {
			if ( StringHelper.isNotEmpty( jaxbEntity.getClazz() ) ) {
				managedClasses.add( XmlProcessingHelper.determineClassName( document, jaxbEntity ) );
			}
			if ( StringHelper.isNotEmpty( jaxbEntity.getName() ) ) {
				managedNames.add( jaxbEntity.getName() );
			}
		} );
	}
}
