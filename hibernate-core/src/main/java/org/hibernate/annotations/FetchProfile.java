/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import jakarta.persistence.FetchType;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static jakarta.persistence.FetchType.EAGER;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.hibernate.annotations.FetchMode.JOIN;

/**
 * Defines a fetch profile, by specifying its {@link #name}, together
 * with a list of {@linkplain #fetchOverrides fetch strategy overrides}.
 * The definition of a single named fetch profile may be split over
 * multiple {@link FetchProfile @FetchProfile} annotations which share
 * the same {@link #name}.
 * <p>
 * Additional fetch strategy overrides may be added to a named fetch
 * profile by annotating the fetched associations themselves with the
 * {@link FetchProfileOverride @FetchProfileOverride} annotation,
 * specifying the {@linkplain FetchProfileOverride#profile name of the
 * fetch profile}.
 * <p>
 * A named fetch profile must be explicitly enabled in a given session
 * by calling {@link org.hibernate.Session#enableFetchProfile(String)}
 * for it to have any effect at runtime.
 * <p>
 * Fetch profiles compete with JPA-defined
 * {@linkplain jakarta.persistence.NamedEntityGraph named entity graphs},
 * and so programs which wish to maintain compatibility with alternative
 * implementations of JPA should prefer {@code @NamedEntityGraph}. The
 * semantics of these two facilities are not quite identical, however,
 * since a fetch profile is a list, not a graph.
 * <p>
 * Or, if we insist on thinking in terms of graphs:
 * <ul>
 * <li>for a fetch profile, the graph is implicit, determined by
 *     recursively following fetched associations from the root entity,
 *     and each {@link FetchOverride} in the fetch profile applies the
 *     same fetching strategy to the overridden association wherever it
 *     is reached recursively within the graph, whereas
 * <li>an entity graph is explicit, and simply specifies that each path
 *     from the root of the graph should be fetched.
 * </ul>
 * <p>
 * However, a fetch profile is not by nature rooted at any one particular
 * entity, and so {@code @FetchProfile} is not required to annotate the
 * entity classes it affects. It may even occur as a package-level
 * annotation.
 * <p>
 * Instead, the root entity of a fetch graph is determined by the context
 * in which the fetch profile is active. For example, if a fetch profile
 * is active when {@link org.hibernate.Session#get(Class, Object)} is
 * called, then the root entity is the entity with the given {@link Class}.
 * Given a root entity as input, an active fetch profile contributes to
 * the determination of the fetch graph.
 * <p>
 * A JPA {@link jakarta.persistence.EntityGraph} may be constructed in
 * Java code at runtime. But this amounts to a separate, albeit extremely
 * limited, query facility that competes with JPA's own {@linkplain
 * jakarta.persistence.criteria.CriteriaBuilder criteria queries}.
 * There's no such capability for fetch profiles.
 *
 * @see FetchProfileOverride
 * @see org.hibernate.Session#enableFetchProfile(String)
 * @see org.hibernate.SessionFactory#containsFetchProfileDefinition(String)
 *
 * @author Hardy Ferentschik
 */
@Target({TYPE, PACKAGE})
@Retention(RUNTIME)
@Repeatable(FetchProfiles.class)
public @interface FetchProfile {
	/**
	 * The name of the fetch profile. Must be unique within a persistence
	 * unit.
	 *
	 * @see org.hibernate.SessionFactory#getDefinedFetchProfileNames()
	 */
	String name();

	/**
	 * The list of association fetching strategy overrides.
	 * <p>
	 * Additional overrides may be specified by marking the
	 * fetched associations themselves with the {@link Fetch @Fetch}
	 * annotation.
	 */
	FetchOverride[] fetchOverrides() default {};

	/**
	 * Overrides the fetching strategy for a particular association in
	 * the named fetch profile being defined. A "strategy" is a fetching
	 * {@linkplain #mode method}, together with the {@linkplain #fetch
	 * timing}. If {@link #mode} and {@link #fetch} are both unspecified,
	 * the strategy defaults to {@linkplain FetchType#EAGER eager}
	 * {@linkplain FetchMode#JOIN join} fetching.
	 * <p>
	 * Additional fetch strategy overrides may be specified using the
	 * {@link FetchProfileOverride @FetchProfileOverride} annotation.
	 *
	 * @see FetchProfileOverride
	 */
	@Target({ TYPE, PACKAGE })
	@Retention(RUNTIME)
	@interface FetchOverride {
		/**
		 * The entity containing the association whose default fetch
		 * strategy is being overridden.
		 */
		Class<?> entity();

		/**
		 * The association whose default fetch strategy is being
		 * overridden.
		 */
		String association();

		/**
		 * The {@linkplain FetchMode method} used for fetching the
		 * association in the fetch profile being defined.
		 */
		FetchMode mode() default JOIN;

		/**
		 * The {@link FetchType timing} of association fetching in
		 * the fetch profile being defined.
		 */
		FetchType fetch() default EAGER;
	}
}
