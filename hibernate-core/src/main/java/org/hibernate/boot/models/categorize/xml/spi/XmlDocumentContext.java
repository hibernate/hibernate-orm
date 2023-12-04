/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.xml.spi;

import org.hibernate.models.spi.SourceModelBuildingContext;

/**
 * Context to a specific XML mapping file
 * @author Steve Ebersole
 */
public interface XmlDocumentContext {
	XmlDocument getXmlDocument();

	PersistenceUnitMetadata getPersistenceUnitMetadata();

	SourceModelBuildingContext getModelBuildingContext();
}
