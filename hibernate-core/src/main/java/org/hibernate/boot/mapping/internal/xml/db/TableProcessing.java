/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.xml.db;

import org.hibernate.boot.jaxb.mapping.spi.JaxbJoinTableImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.JoinTableJpaAnnotation;
import org.hibernate.boot.mapping.internal.xml.XmlDocumentContext;
import org.hibernate.models.spi.MutableAnnotationTarget;

/**
 * @author Steve Ebersole
 */
public class TableProcessing {
	public static JoinTableJpaAnnotation transformJoinTable(
			JaxbJoinTableImpl jaxbJoinTable,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbJoinTable == null ) {
			return null;
		}

		final JoinTableJpaAnnotation joinTableUsage = (JoinTableJpaAnnotation) target.applyAnnotationUsage(
				JpaAnnotations.JOIN_TABLE,
				xmlDocumentContext.getModelBuildingContext()
		);
		joinTableUsage.apply( jaxbJoinTable, xmlDocumentContext );
		return joinTableUsage;
	}

}
