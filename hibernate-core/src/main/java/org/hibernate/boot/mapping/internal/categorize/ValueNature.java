/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

/// The semantic shape of a categorized value.
///
/// Unlike [org.hibernate.boot.models.AttributeNature], this enum does not
/// describe plural-attribute declarations.  A collection element or map key
/// may have any of these natures, but cannot itself be an element collection,
/// one-to-many, or many-to-many attribute.
///
/// @since 9.0
/// @author Steve Ebersole
public enum ValueNature {
	BASIC,
	EMBEDDED,
	TO_ONE,
	ANY
}
