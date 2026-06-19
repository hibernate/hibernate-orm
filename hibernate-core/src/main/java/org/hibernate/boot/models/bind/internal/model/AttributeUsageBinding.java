/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.model;

import org.hibernate.boot.models.AttributeNature;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;

/// Binding-model contract for one concrete usage of an attribute declaration.
///
/// A usage binding describes the resolved mapping context where an attribute is
/// used: source member, resolved type, path/source role, value intent, and
/// broad attribute nature.  Even when the declaration container and usage
/// container are the same managed type, declaration and usage remain distinct
/// binding-model roles.
///
/// @since 9.0
/// @author Steve Ebersole
public interface AttributeUsageBinding {
	/// The persistent attribute name for this usage.
	///
	/// This normally delegates to the source declaration name.  It is distinct
	/// from [#attributePath()], which may include nesting.
	String attributeName();

	/// The declaration that this usage realizes.
	///
	/// This is intentionally non-null, even for embeddable-local members.  When a
	/// declaration is usage-site-sensitive, the declaration records the source
	/// member while this usage owns the effective contextual facts.
	AttributeDeclarationBinding declaration();

	/// The concrete context in which the declaration is used.
	///
	/// This may be a managed type, an embedded component site, or another
	/// usage-specific container introduced by later binding slices.
	AttributeUsageContainer usageContainer();

	/// The source member selected for this concrete usage.
	///
	/// For direct identifiable attributes this often matches the declaration
	/// member.  For embedded or generic contexts it is the member as interpreted
	/// for this usage.
	MemberDetails member();

	/// The member type resolved for this usage.
	///
	/// This may differ from the declaration member type when generics are
	/// specialized by a subtype or embedded site.
	TypeDetails resolvedType();

	/// Diagnostic/source role for this usage.
	///
	/// The role should be stable enough for error messages and contribution
	/// ownership, but it is not necessarily the same as a materialized runtime
	/// role.
	String sourceRole();

	/// Usage-relative attribute path.
	///
	/// Unlike [#attributeName()], this may include nested path segments used for
	/// override, conversion, and naming decisions.
	String attributePath();

	/// Broad persistent attribute kind for this usage.
	AttributeNature nature();

	/// Source-level value intent for this usage, when this slice records one.
	///
	/// Some attribute kinds are still transitional and may return `null` until
	/// their value facts are lifted into this binding model.
	ValueIntent valueIntent();

	/// Narrow [#valueIntent()] to a basic-valued intent when applicable.
	default BasicValueIntent basicValueIntent() {
		return valueIntent() instanceof BasicValueIntent basicValueIntent ? basicValueIntent : null;
	}

	/// Narrow [#valueIntent()] to an embedded-valued intent when applicable.
	default EmbeddedValueIntent embeddedValueIntent() {
		return valueIntent() instanceof EmbeddedValueIntent embeddedValueIntent ? embeddedValueIntent : null;
	}

	/// Narrow [#valueIntent()] to a to-one-valued intent when applicable.
	default ToOneValueIntent toOneValueIntent() {
		return valueIntent() instanceof ToOneValueIntent toOneValueIntent ? toOneValueIntent : null;
	}
}
