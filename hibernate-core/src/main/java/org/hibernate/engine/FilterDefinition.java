/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine;

import java.util.Set;

/// Descriptive metadata for a named [org.hibernate.Filter].
/// Obtain a definition from [org.hibernate.SessionFactory#getFilterDefinition(String)]
/// or [org.hibernate.Filter#getFilterDefinition()].
///
/// @see org.hibernate.annotations.FilterDef
///
/// @since 8.0
///
/// @author Steve Ebersole
public interface FilterDefinition {

	/// The name identifying this filter definition.
	String getFilterName();

	/// The names of the parameters declared by this filter definition.
	Set<String> getParameterNames();

	/// The default SQL condition, which individual filter mappings may override.
	String getDefaultFilterCondition();

	/// Whether this filter is enabled automatically for a session.
	boolean isAutoEnabled();

	/// Whether this filter applies when loading an entity directly by its key.
	boolean isAppliedToLoadByKey();
}
