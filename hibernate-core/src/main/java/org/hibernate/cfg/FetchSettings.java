/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

import org.hibernate.annotations.BatchSize;

/**
 * @author Steve Ebersole
 */
public interface FetchSettings {
	/**
	 * Specifies the maximum depth of nested outer join fetching.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyMaximumFetchDepth
	 *
	 * @settingDefault 0 (none)
	 */
	String MAX_FETCH_DEPTH = "hibernate.max_fetch_depth";

	/**
	 * Specifies the default value for {@linkplain BatchSize#size() batch fetching}.
	 * <p/>
	 * By default, Hibernate only uses batch fetching for entities and collections explicitly
	 * annotated {@code @BatchSize}.
	 *
	 * @see org.hibernate.annotations.BatchSize
	 * @see org.hibernate.Session#setFetchBatchSize(int)
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyDefaultBatchFetchSize(int)
	 */
	String DEFAULT_BATCH_FETCH_SIZE = "hibernate.default_batch_fetch_size";

	/**
	 * When enabled, Hibernate will use subselect fetching, when possible, to
	 * fetch any collection.  Subselect fetching involves fetching the collection
	 * based on the restriction used to load it owner(s).
	 * <p>
	 * By default, Hibernate only uses subselect fetching for collections
	 * explicitly annotated {@linkplain org.hibernate.annotations.FetchMode#SUBSELECT @Fetch(SUBSELECT)}.
	 *
	 * @since 6.3
	 *
	 * @see org.hibernate.annotations.FetchMode#SUBSELECT
	 * @see org.hibernate.Session#setSubselectFetchingEnabled(boolean)
	 * @see org.hibernate.boot.SessionFactoryBuilder#applySubselectFetchEnabled(boolean)
	 */
	String USE_SUBSELECT_FETCH = "hibernate.use_subselect_fetch";
}
