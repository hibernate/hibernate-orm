/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.spi;

/// Semantic shape of a categorized [ValueMetadata].
///
/// @since 9.0
/// @author Steve Ebersole
public enum ValueNature {
	/// A basic, converted, or custom-typed value.
	BASIC,
	/// An aggregate or composite value described by embeddable metadata.
	EMBEDDED,
	/// A many-to-one or one-to-one association.
	TO_ONE,
	/// A polymorphic association using Hibernate's `@Any` mapping.
	ANY
}
