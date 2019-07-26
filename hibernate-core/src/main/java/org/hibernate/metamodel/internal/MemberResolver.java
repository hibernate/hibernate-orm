/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Member;

/**
 * Contract for how we resolve the {@link Member} for a give attribute context.
 */
public interface MemberResolver {
	Member resolveMember(AttributeContext attributeContext, MetadataContext metadataContext);
}
