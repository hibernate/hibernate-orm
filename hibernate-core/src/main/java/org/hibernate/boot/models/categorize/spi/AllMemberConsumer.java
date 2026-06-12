/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import org.hibernate.models.spi.MemberDetails;

/// Callback used while resolving persistent attributes to observe every member
/// declared by a managed class.
///
/// The persistent attribute resolver uses this hook for consumers that need to
/// inspect non-persistent members while the class is already being scanned.
///
/// @since 9.0
/// @author Steve Ebersole
@FunctionalInterface
public interface AllMemberConsumer {
	/// Visit a member declared by the class being inspected.
	void acceptMember(MemberDetails memberDetails);
}
