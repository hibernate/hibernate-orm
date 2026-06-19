/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import org.hibernate.boot.jaxb.mapping.spi.JaxbJavaTypeRegistrationImpl;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.type.descriptor.java.JavaType;

/// Global registration for a {@linkplain JavaType Java type descriptor}.
///
/// @param domainType The Java type handled by the descriptor
/// @param descriptor The Java type descriptor class
///
/// @see org.hibernate.annotations.JavaTypeRegistration
/// @see JaxbJavaTypeRegistrationImpl
///
/// @since 9.0
/// @author Steve Ebersole
public record JavaTypeRegistration(ClassDetails domainType, ClassDetails descriptor) {
}
