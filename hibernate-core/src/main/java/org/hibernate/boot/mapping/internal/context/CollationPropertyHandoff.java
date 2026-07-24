/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.context;

import org.hibernate.boot.mapping.internal.view.CollationContributionView;
import org.hibernate.mapping.Property;

/// Links a semantic collation contribution to its materialized compatibility
/// property.
///
/// @since 9.0
/// @author Steve Ebersole
public record CollationPropertyHandoff(
		CollationContributionView contribution,
		Property property) {
}
