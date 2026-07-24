/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import org.hibernate.annotations.CompositeTypeRegistration;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCompositeUserTypeRegistrationImpl;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.usertype.CompositeUserType;

/// Global registration for a {@linkplain CompositeUserType composite user type}.
///
/// @param embeddableClass The embeddable class handled by the composite user type
/// @param userTypeClass The composite user type class
///
/// @see CompositeTypeRegistration
/// @see JaxbCompositeUserTypeRegistrationImpl
///
/// @since 9.0
/// @author Steve Ebersole
public record CompositeUserTypeRegistration(
		ClassDetails embeddableClass,
		ClassDetails userTypeClass) {
}
