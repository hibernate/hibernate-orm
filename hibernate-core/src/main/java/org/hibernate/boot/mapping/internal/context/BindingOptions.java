/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.context;

import java.util.EnumSet;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.mapping.internal.relational.QuotedIdentifierTarget;

import jakarta.persistence.FetchType;

/// Immutable options that affect how categorized metadata is bound.
///
/// Options here are values computed before binding starts, such as default schema
/// names and the identifier kinds affected by global quoting.
///
/// @since 9.0
/// @author Steve Ebersole
public interface BindingOptions {
	/// Default catalog name to use when a mapping does not specify one.
	Identifier getDefaultCatalogName();

	/// Default schema name to use when a mapping does not specify one.
	Identifier getDefaultSchemaName();

	/// Identifier categories that should be globally quoted during binding.
	EnumSet<QuotedIdentifierTarget> getGloballyQuotedIdentifierTargets();

	/// Whether joined inheritance should create implicit discriminator mappings.
	boolean createImplicitDiscriminatorsForJoinedInheritance();

	/// Whether explicit joined-inheritance discriminator mappings should be ignored.
	boolean ignoreExplicitDiscriminatorsForJoinedInheritance();

	/// Whether discriminator mappings should be forced into select statements by
	/// default.
	boolean shouldImplicitlyForceDiscriminatorInSelect();

	/// Default fetch type to use for to-one associations when no explicit fetch
	/// mode is distinguishable from the annotation default.
	FetchType getDefaultToOneFetchType();
}
