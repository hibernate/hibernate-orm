/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.model;

import org.hibernate.boot.models.AttributeNature;

/// Source-level intent for a value contributed by a binding-model node.
///
/// This is the common binding-model contract shared by managed-type
/// [IdentifiableAttributeDeclarationBinding] nodes and embeddable [ComponentMemberBinding]
/// nodes.  Implementations describe source-model value intent without retaining
/// compatibility objects produced by later materialization phases.
///
/// @since 9.0
/// @author Steve Ebersole
public interface ValueIntent {
	AttributeNature nature();
}
