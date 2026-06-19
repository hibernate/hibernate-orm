/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.categorize;

import java.util.Map;

import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionUserTypeImpl;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.usertype.UserCollectionType;

/// Global registration for a {@linkplain UserCollectionType custom collection type}.
///
/// @param classification The collection classification the custom type handles
/// @param userTypeClass The custom collection type class
/// @param parameterMap Parameters supplied to the custom type
///
/// @see org.hibernate.annotations.CollectionTypeRegistration
/// @see JaxbCollectionUserTypeImpl
///
/// @since 9.0
/// @author Steve Ebersole
public record CollectionTypeRegistration(
		CollectionClassification classification,
		ClassDetails userTypeClass,
		Map<String, String> parameterMap) {
}
