/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.context;

import org.hibernate.boot.mapping.internal.view.TenantIdBindingView;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;

/// Links a semantic tenant-id binding to its materialized compatibility
/// property and owning entity root.
///
/// @since 9.0
/// @author Steve Ebersole
public record TenantIdPropertyHandoff(
		TenantIdBindingView binding,
		RootClass rootClass,
		Property property) {
}
