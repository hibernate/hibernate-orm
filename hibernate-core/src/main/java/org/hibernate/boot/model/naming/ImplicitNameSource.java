/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;

/// Naming context shared by the remaining Identifier-based source contracts.
///
/// @author Steve Ebersole
public interface ImplicitNameSource {
	/// Focused defaults and identifier helpers for this naming decision.
	///
	/// @return The naming context
	ImplicitNamingContext getNamingContext();
}
