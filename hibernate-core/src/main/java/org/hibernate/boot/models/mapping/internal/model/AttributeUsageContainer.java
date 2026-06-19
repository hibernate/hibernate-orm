/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.model;

/// Binding-model container in which an attribute declaration is used.
///
/// Managed types are usage containers for direct and inherited entity or
/// mapped-superclass attributes.  Embedded sites, collection elements, and map
/// keys are also usage containers even though they are not managed type
/// declarations.
///
/// @since 9.0
/// @author Steve Ebersole
public interface AttributeUsageContainer {
	/// Human-readable role for this usage container.
	///
	/// The role is used to build source roles, diagnostics, and ownership links
	/// in the binding model.  It should describe the usage context without
	/// implying a specific materialization target.
	String usageRole();
}
