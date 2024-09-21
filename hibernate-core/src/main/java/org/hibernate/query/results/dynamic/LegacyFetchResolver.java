/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.dynamic;

import org.hibernate.query.NativeQuery;

/**
 * Contract for handling Hibernate's legacy way of representing fetches through
 * {@link NativeQuery#addFetch}, {@link NativeQuery#addJoin},
 * `hbm.xml` mappings, etc
 *
 * @see org.hibernate.query.results.DomainResultCreationStateImpl#getLegacyFetchResolver()
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface LegacyFetchResolver {
	DynamicFetchBuilderLegacy resolve(String ownerTableAlias, String fetchedPartPath);
}
