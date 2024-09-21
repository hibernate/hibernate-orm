/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph;

import java.util.Locale;

import org.hibernate.AssertionFailure;

import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_FETCH_GRAPH;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_LOAD_GRAPH;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_FETCH_GRAPH;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOAD_GRAPH;

/**
 * JPA specifies two distinct ways to apply an {@link jakarta.persistence.EntityGraph} -
 * as a {@link #FETCH "fetch graph"} or as a {@link #LOAD "load graph"}.
 *
 * @author Steve Ebersole
 */
public enum GraphSemantic {
	/**
	 * Indicates that an {@link jakarta.persistence.EntityGraph} should be interpreted as a JPA "fetch graph".
	 * <ul>
	 * <li>Attributes explicitly specified using an {@link org.hibernate.graph.AttributeNode}s are treated as
	 * {@link jakarta.persistence.FetchType#EAGER} and fetched via a join or subsequent select.
	 * <li>Attributes not explicitly specified are treated as {@link jakarta.persistence.FetchType#LAZY} and
	 * are not fetched.
	 * </ul>
	 */
	FETCH,

	/**
	 * Indicates that an {@link jakarta.persistence.EntityGraph} should be interpreted as a JPA "load graph".
	 * <ul>
	 * <li>Attributes explicitly specified using an {@link org.hibernate.graph.AttributeNode}s are treated as
	 * {@link jakarta.persistence.FetchType#EAGER} and fetched via a join or subsequent select.
	 * <li>Attributes not explicitly specified are treated as {@code FetchType.LAZY} or {@code FetchType.EAGER}
	 * depending on the mapping of the attribute, instead of forcing {@code FetchType.LAZY}.
	 * </ul>
	 */
	LOAD;

	/**
	 * The corresponding Jakarta Persistence hint name.
	 *
	 * @see org.hibernate.jpa.SpecHints#HINT_SPEC_FETCH_GRAPH
	 * @see org.hibernate.jpa.SpecHints#HINT_SPEC_LOAD_GRAPH
	 */
	public String getJakartaHintName() {
		switch ( this ) {
			case FETCH:
				return HINT_SPEC_FETCH_GRAPH;
			case LOAD:
				return HINT_SPEC_LOAD_GRAPH;
			default:
				throw new AssertionFailure( "unknown GraphSemantic" );
		}
	}

	/**
	 * The hint name that should be used with Java Persistence.
	 *
	 * @see org.hibernate.jpa.LegacySpecHints#HINT_JAVAEE_FETCH_GRAPH
	 * @see org.hibernate.jpa.LegacySpecHints#HINT_JAVAEE_LOAD_GRAPH
	 *
	 * @deprecated Use {@link #getJakartaHintName} instead
	 */
	@Deprecated(since = "6.0")
	public String getJpaHintName() {
		switch ( this ) {
			case FETCH:
				return HINT_JAVAEE_FETCH_GRAPH;
			case LOAD:
				return HINT_JAVAEE_LOAD_GRAPH;
			default:
				throw new AssertionFailure( "unknown GraphSemantic" );
		}
	}

	public static GraphSemantic fromHintName(String hintName) {
		switch ( hintName ) {
			case HINT_SPEC_FETCH_GRAPH:
			case HINT_JAVAEE_FETCH_GRAPH:
				return FETCH;
			case HINT_SPEC_LOAD_GRAPH:
			case HINT_JAVAEE_LOAD_GRAPH:
				return LOAD;
			default:
				throw new IllegalArgumentException(
						String.format(
								Locale.ROOT,
								"Unknown EntityGraph hint name - `%s`.  " +
										"Expecting `%s` or `%s` (or `%s` and `%s`).",
								hintName,
								HINT_SPEC_FETCH_GRAPH,
								HINT_SPEC_LOAD_GRAPH,
								HINT_JAVAEE_FETCH_GRAPH,
								HINT_JAVAEE_LOAD_GRAPH
						)
				);
		}
	}

	/**
	 * @deprecated Use {@link #fromHintName} instead
	 */
	@Deprecated(since = "6.0")
	public static GraphSemantic fromJpaHintName(String hintName) {
		return fromHintName( hintName );
	}
}
