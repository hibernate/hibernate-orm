/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

/**
 * Enumerates methods for fetching an association from the database.
 * <p>
 * The JPA-defined {@link jakarta.persistence.FetchType} enumerates the
 * possibilities for <em>when</em> an association might be fetched. This
 * annotation defines <em>how</em> it is fetched in terms of the actual
 * SQL executed by the database.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 *
 * @see Fetch
 * @see FetchProfile.FetchOverride#mode()
 */
public enum FetchMode {
	/**
	 * Use a secondary select to load a single associated entity or
	 * collection, at some point after an initial query is executed.
	 * <p>
	 * This is the default fetching strategy for any association
	 * or collection in Hibernate, unless the association or
	 * collection is {@linkplain jakarta.persistence.FetchType#EAGER
	 * explicitly marked for eager fetching}.
	 * <p>
	 * This fetching strategy is vulnerable to the "N+1 selects"
	 * bugbear, though the impact may be alleviated somewhat via:
	 * <ul>
	 * <li>enabling batch fetching using {@link BatchSize}, or
	 * <li>ensuring that the associated entity or collection may be
	 *     retrieved from the {@linkplain Cache second-level cache}.
	 * </ul>
	 * <p>
	 * This fetching strategy is, in principle, compatible with both
	 * {@linkplain jakarta.persistence.FetchType#EAGER eager} and
	 * {@linkplain jakarta.persistence.FetchType#LAZY lazy} fetching.
	 * On the other hand, performance considerations dictate that it
	 * should only be used in combination with {@code EAGER} fetching
	 * when it is almost certain that the associated data will be
	 * available in the second-level cache.
	 */
	SELECT,

	/**
	 * Use an outer join to load all instances of the related entity
	 * or collection at once, as part of the execution of a query.
	 * No subsequent queries are executed.
	 * <p>
	 * This is the default fetching strategy for an association or
	 * collection that is {@linkplain jakarta.persistence.FetchType#EAGER
	 * explicitly marked for eager fetching}.
	 * <p>
	 * This fetching strategy is incompatible with
	 * {@linkplain  jakarta.persistence.FetchType#LAZY lazy fetching}
	 * since the associated data is retrieved as part of the initial
	 * query.
	 */
	JOIN,

	/**
	 * Use a secondary select with a subselect that re-executes an
	 * initial query to load all instances of the related entity or
	 * collection at once, at some point after the initial query is
	 * executed. This fetching strategy is currently only available
	 * for collections and many-valued associations.
	 * <p>
	 * This advanced fetching strategy is compatible with both
	 * {@linkplain jakarta.persistence.FetchType#EAGER eager} and
	 * {@linkplain jakarta.persistence.FetchType#LAZY lazy} fetching.
	 * <p>
	 * Subselect fetching may be contrasted with {@linkplain BatchSize
	 * batch fetching}:
	 * <ul>
	 * <li>In batch fetching, a list of primary key values is sent to
	 *     the database, bound within a SQL {@code in} condition.
	 * <li>In subselect fetching, the primary keys are determined by
	 *     re-execution of the initial query within a SQL subselect.
	 * </ul>
	 */
	SUBSELECT;

	public org.hibernate.FetchMode getHibernateFetchMode() {
		return this == JOIN
				? org.hibernate.FetchMode.JOIN
				: org.hibernate.FetchMode.SELECT;
	}
}
