/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Member;

/**
 * Contract for how we resolve the {@link Member} for a give attribute context.
 */
public interface MemberResolver {
	Member resolveMember(AttributeContext attributeContext, MetadataContext metadataContext);
}
