/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata;
import org.hibernate.boot.models.xml.spi.XmlPreProcessingResult;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public class XmlPreProcessingResultImpl implements XmlPreProcessingResult {
	private final PersistenceUnitMetadataImpl persistenceUnitMetadata;
	private final List<JaxbEntityMappingsImpl> documents = new ArrayList<>();
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
			else if ( StringHelper.isNotEmpty( jaxbEmbeddable.getName() ) ) {
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
			else if ( StringHelper.isNotEmpty( jaxbEntity.getName() ) ) {
				managedNames.add( jaxbEntity.getName() );
			}
		} );
	}
}
