/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata;
import org.hibernate.boot.models.xml.spi.XmlDocument;
import org.hibernate.boot.models.xml.spi.XmlPreProcessingResult;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public class XmlPreProcessingResultImpl implements XmlPreProcessingResult {
	private final PersistenceUnitMetadataImpl persistenceUnitMetadata;
	private final List<XmlDocument> documents = new ArrayList<>();
	private final List<String> managedClasses = new ArrayList<>();
	private final List<String> managedNames = new ArrayList<>();

	public XmlPreProcessingResultImpl(PersistenceUnitMetadataImpl persistenceUnitMetadata) {
		this.persistenceUnitMetadata = persistenceUnitMetadata;
	}

	public XmlPreProcessingResultImpl(PersistenceUnitMetadata persistenceUnitMetadata) {
		this( (PersistenceUnitMetadataImpl) persistenceUnitMetadata );
	}

	/**
	 * Intended for testing
	 */
	public XmlPreProcessingResultImpl() {
		this( new PersistenceUnitMetadataImpl() );
	}

	public PersistenceUnitMetadataImpl getPersistenceUnitMetadata() {
		return persistenceUnitMetadata;
	}

	@Override
	public List<XmlDocument> getDocuments() {
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

	public void addDocument(Binding<JaxbEntityMappingsImpl> binding) {
		final XmlDocumentImpl xmlDocument = XmlDocumentImpl.consume( binding, persistenceUnitMetadata );
		documents.add( xmlDocument );

		final JaxbEntityMappingsImpl jaxbRoot = binding.getRoot();
		persistenceUnitMetadata.apply( jaxbRoot.getPersistenceUnitMetadata() );
		jaxbRoot.getEmbeddables().forEach( (jaxbEmbeddable) -> {
			if ( StringHelper.isNotEmpty( jaxbEmbeddable.getClazz() ) ) {
				managedClasses.add( XmlProcessingHelper.determineClassName( jaxbRoot, jaxbEmbeddable ) );
			}
			else if ( StringHelper.isNotEmpty( jaxbEmbeddable.getName() ) ) {
				managedNames.add( jaxbEmbeddable.getName() );
			}
		} );
		jaxbRoot.getMappedSuperclasses().forEach( (jaxbMappedSuperclass) -> {
			managedClasses.add( XmlProcessingHelper.determineClassName( jaxbRoot, jaxbMappedSuperclass ) );
		} );
		jaxbRoot.getEntities().forEach( (jaxbEntity) -> {
			if ( StringHelper.isNotEmpty( jaxbEntity.getClazz() ) ) {
				managedClasses.add( XmlProcessingHelper.determineClassName( jaxbRoot, jaxbEntity ) );
			}
			else if ( StringHelper.isNotEmpty( jaxbEntity.getName() ) ) {
				managedNames.add( jaxbEntity.getName() );
			}
		} );
	}
}
