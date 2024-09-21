/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;

import org.hibernate.metamodel.CollectionClassification;

/**
 * JAXB marshalling for {@link CollectionClassification}
 *
 * @author Steve Ebersole
 */
public class CollectionClassificationMarshalling {
	public static CollectionClassification fromXml(String name) {
		return name == null ? null : CollectionClassification.valueOf( name );
	}

	public static String toXml(CollectionClassification classification) {
		return classification == null ? null : classification.name();
	}
}
