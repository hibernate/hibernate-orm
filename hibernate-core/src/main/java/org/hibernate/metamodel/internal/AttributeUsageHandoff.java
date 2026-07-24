/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.io.Serializable;

import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;

/// Immutable declaration/usage facts needed by runtime metamodel creation.
///
/// @since 9.0
/// @author Steve Ebersole
public record AttributeUsageHandoff(
		MemberDetails member,
		TypeDetails declaredType,
		TypeDetails usageType) implements Serializable {
	public boolean isConcreteGenericUsage() {
		return AttributeTypeCorrespondence.isConcreteGenericUsage( declaredType, usageType );
	}
}
