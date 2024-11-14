/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.dynamic;

import org.hibernate.query.NativeQuery;
import org.hibernate.query.results.internal.DomainResultCreationStateImpl;

/**
 * Contract for handling Hibernate's legacy way of representing fetches through
 * {@link NativeQuery#addFetch}, {@link NativeQuery#addJoin},
 * `hbm.xml` mappings, etc
 *
 * @see DomainResultCreationStateImpl#getLegacyFetchResolver()
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface LegacyFetchResolver {
	DynamicFetchBuilderLegacy resolve(String ownerTableAlias, String fetchedPartPath);
}
