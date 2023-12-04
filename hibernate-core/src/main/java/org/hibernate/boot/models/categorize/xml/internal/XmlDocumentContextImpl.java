/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.xml.internal;

import org.hibernate.boot.models.categorize.xml.spi.PersistenceUnitMetadata;
import org.hibernate.boot.models.categorize.xml.spi.XmlDocument;
import org.hibernate.boot.models.categorize.xml.spi.XmlDocumentContext;
import org.hibernate.models.spi.SourceModelBuildingContext;

/**
 * @author Steve Ebersole
 */
public class XmlDocumentContextImpl implements XmlDocumentContext {
	private final XmlDocument xmlDocument;
	private final PersistenceUnitMetadata persistenceUnitMetadata;
	private final SourceModelBuildingContext modelBuildingContext;

	public XmlDocumentContextImpl(
			XmlDocument xmlDocument,
			PersistenceUnitMetadata persistenceUnitMetadata,
			SourceModelBuildingContext modelBuildingContext) {
		this.xmlDocument = xmlDocument;
		this.persistenceUnitMetadata = persistenceUnitMetadata;
		this.modelBuildingContext = modelBuildingContext;
	}

	@Override
	public XmlDocument getXmlDocument() {
		return xmlDocument;
	}

	@Override
	public PersistenceUnitMetadata getPersistenceUnitMetadata() {
		return persistenceUnitMetadata;
	}

	@Override
	public SourceModelBuildingContext getModelBuildingContext() {
		return modelBuildingContext;
	}
}
