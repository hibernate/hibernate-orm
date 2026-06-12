/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import org.hibernate.annotations.TypeRegistration;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.usertype.UserType;

/// Global registration for a {@linkplain UserType user type}.
///
/// @param domainClass The domain Java type handled by the user type
/// @param userTypeClass The user type implementation class
///
/// @see TypeRegistration
///
/// @since 9.0
/// @author Steve Ebersole
public record UserTypeRegistration(ClassDetails domainClass, ClassDetails userTypeClass) {
}
