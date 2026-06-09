/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import org.hibernate.models.spi.ClassDetails;

/// Global registration for a JPA `AttributeConverter` class.
///
/// @param converterType The converter implementation class
/// @param autoApply An XML override for auto-apply, or {@code null} to use the converter annotation
///
/// @since 9.0
/// @author Steve Ebersole
public record JpaConverterRegistration(ClassDetails converterType, Boolean autoApply) {
}
