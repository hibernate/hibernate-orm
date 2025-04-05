/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.spi;

import java.util.List;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManagedType;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMappedSuperclassImpl;

/**
 * Collected XML override mappings we can apply wholesale after
 * processing metadata-complete mappings and annotations.
 *
 * @author Steve Ebersole
 */
public interface XmlProcessingResult {
	/**
	 * Tuple of an override descriptor ({@code managedType}) along with the JAXB root it came from
	 */
	class OverrideTuple<M extends JaxbManagedType> {
		private final JaxbEntityMappingsImpl jaxbRoot;
		private final XmlDocumentContext xmlDocumentContext;
		private final M managedType;

		public OverrideTuple(JaxbEntityMappingsImpl jaxbRoot, XmlDocumentContext xmlDocumentContext, M managedType) {
			this.jaxbRoot = jaxbRoot;
			this.xmlDocumentContext = xmlDocumentContext;
			this.managedType = managedType;
		}

		public JaxbEntityMappingsImpl getJaxbRoot() {
			return jaxbRoot;
		}

		public XmlDocumentContext getXmlDocumentContext() {
			return xmlDocumentContext;
		}

		public M getManagedType() {
			return managedType;
		}
	}

	void apply();

	List<OverrideTuple<JaxbEntityImpl>> getEntityOverrides();
	List<OverrideTuple<JaxbMappedSuperclassImpl>> getMappedSuperclassesOverrides();
	List<OverrideTuple<JaxbEmbeddableImpl>> getEmbeddableOverrides();
}
