/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.xml.internal.attr;

import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToOneImpl;
import org.hibernate.boot.models.categorize.xml.spi.XmlDocumentContext;
import org.hibernate.models.internal.MutableClassDetails;
import org.hibernate.models.internal.MutableMemberDetails;

import jakarta.persistence.AccessType;

/**
 * @author Steve Ebersole
 */
public class OneToOneAttributeProcessing {

	@SuppressWarnings("UnusedReturnValue")
	public static MutableMemberDetails processOneToOneAttribute(
			JaxbOneToOneImpl jaxbOneToOne,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		throw new UnsupportedOperationException( "Support for one-to-one attributes not yet implemented" );
	}
}
