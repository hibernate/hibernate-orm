/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal;

import org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata;
import org.hibernate.boot.models.xml.spi.XmlDocument;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
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
