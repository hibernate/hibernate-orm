/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

/// Entity names made available to implicit naming algorithms.
/// Hibernate's mapping name and the Jakarta Persistence entity name serve different
/// purposes and need not be identical. No Java class loading is required.
///
/// @author Steve Ebersole
public interface EntityNaming {
	/// The fully qualified mapped Java class name, when available.
	///
	/// @return The class name, or null for a mapping without a Java class name
	String getClassName();

	/// The Hibernate entity name identifying the mapping, commonly its qualified class name.
	///
	/// @return The Hibernate entity name
	String getEntityName();

	/// The effective Jakarta Persistence entity name, including its default when supplied
	/// by the mapping source. This does not indicate whether the name was explicit.
	///
	/// @return The Jakarta Persistence entity name, or null when unavailable
	/// @see jakarta.persistence.Entity#name()
	String getJpaEntityName();
}
