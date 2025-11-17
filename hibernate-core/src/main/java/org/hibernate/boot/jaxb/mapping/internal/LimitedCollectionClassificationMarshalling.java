/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;


import org.hibernate.boot.internal.LimitedCollectionClassification;

import jakarta.persistence.AccessType;

/**
 * JAXB marshalling for JPA's {@link AccessType}
 *
 * @author Steve Ebersole
 */
public class LimitedCollectionClassificationMarshalling {
	public static LimitedCollectionClassification fromXml(String name) {
		return name == null ? null : LimitedCollectionClassification.valueOf( name );
	}

	public static String toXml(LimitedCollectionClassification classification) {
		return classification == null ? null : classification.name();
	}
}
