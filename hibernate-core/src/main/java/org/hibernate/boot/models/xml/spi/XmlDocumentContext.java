/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.spi;

import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.models.internal.MutableClassDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;

/**
 * Context for a specific XML mapping file
 *
 * @author Steve Ebersole
 */
public interface XmlDocumentContext {
	/**
	 * The XML document
	 */
	XmlDocument getXmlDocument();

	/**
	 * The {@code <persistence-unit-metadata/>} defined by the XML document
	 */
	PersistenceUnitMetadata getPersistenceUnitMetadata();

	/**
	 * Access to the containing context
	 */
	SourceModelBuildingContext getModelBuildingContext();

	/**
	 * Resolve a ClassDetails by name, accounting for XML-defined package name if one.
	 */
	default MutableClassDetails resolveJavaType(String name) {
		return (MutableClassDetails) XmlAnnotationHelper.resolveJavaType( name, this );
	}
}
