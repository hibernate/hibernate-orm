/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import jakarta.persistence.Timeout;

import java.util.Map;

/// Serves the role of [jakarta.persistence.Reference] which is
/// unfortunately sealed, and so we cannot directly extend.  But this
/// allows us to define an extension commonality.
///
/// @author Steve Ebersole
public interface JpaReference {
	String getName();

	Timeout getTimeout();

	Map<String, Object> getHints();
}
