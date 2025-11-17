/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;

/**
 * Base class for any and all source objects coming from {@code hbm.xml} parsing.  Defines standard access
 * back to the {@link MappingDocument} object and the services it provides (namely access to
 * {@link HbmLocalMetadataBuildingContext}).
 *
 * @author Steve Ebersole
 */
public abstract class AbstractHbmSourceNode {
	private final MappingDocument sourceMappingDocument;

	protected AbstractHbmSourceNode(MappingDocument sourceMappingDocument) {
		this.sourceMappingDocument = sourceMappingDocument;
	}

	protected MappingDocument sourceMappingDocument() {
		return sourceMappingDocument;
	}

	protected HbmLocalMetadataBuildingContext metadataBuildingContext() {
		return sourceMappingDocument;
	}

	protected Origin origin() {
		return sourceMappingDocument().getOrigin();
	}

	protected JaxbHbmHibernateMapping mappingRoot() {
		return sourceMappingDocument().getDocumentRoot();
	}
}
