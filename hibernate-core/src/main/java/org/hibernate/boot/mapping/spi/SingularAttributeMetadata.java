/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.spi;

import org.hibernate.boot.models.AttributeNature;
import org.hibernate.models.spi.TypeDetails;

/// Categorized attribute which maps a single [ValueMetadata].
///
/// @since 9.0
/// @author Steve Ebersole
public interface SingularAttributeMetadata extends AttributeMetadata {
	/// The mapped value described by this attribute.
	ValueMetadata getValue();

	@Override
	default TypeDetails getAttributeType() {
		return getValue().getType();
	}

	@Override
	default AttributeNature getNature() {
		return switch ( getValue().getNature() ) {
			case BASIC -> AttributeNature.BASIC;
			case EMBEDDED -> AttributeNature.EMBEDDED;
			case TO_ONE -> AttributeNature.TO_ONE;
			case ANY -> AttributeNature.ANY;
		};
	}
}
