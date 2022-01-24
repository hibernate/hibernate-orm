/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph;

import java.util.Locale;

import org.hibernate.jpa.LegacySpecHints;

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
	FETCH( HINT_SPEC_FETCH_GRAPH, LegacySpecHints.HINT_JAVAEE_FETCH_GRAPH ),

	/**
	 * Indicates that an {@link jakarta.persistence.EntityGraph} should be interpreted as a JPA "load graph".
	 * <ul>
	 * <li>Attributes explicitly specified using an {@link org.hibernate.graph.AttributeNode}s are treated as
	 * {@link jakarta.persistence.FetchType#EAGER} and fetched via a join or subsequent select.
	 * <li>Attributes not explicitly specified are treated as {@code FetchType.LAZY} or {@code FetchType.EAGER}
	 * depending on the mapping of the attribute, instead of forcing {@code FetchType.LAZY}.
	 * </ul>
	 */
	LOAD( HINT_SPEC_LOAD_GRAPH, LegacySpecHints.HINT_JAVAEE_LOAD_GRAPH );

	private final String jakartaHintName;
	private final String javaeeHintName;

	GraphSemantic(String jakartaHintName, String javaeeHintName) {
		this.jakartaHintName = jakartaHintName;
		this.javaeeHintName = javaeeHintName;
	}

	/**
	 * The corresponding Jakarta Persistence hint name.
	 *
	 * @see org.hibernate.jpa.SpecHints#HINT_SPEC_FETCH_GRAPH
	 * @see org.hibernate.jpa.SpecHints#HINT_SPEC_LOAD_GRAPH
	 */
	public String getJakartaHintName() {
		return jakartaHintName;
	}

	/**
	 * The hint name that should be used with Java Persistence.
	 *
	 * @see org.hibernate.jpa.LegacySpecHints#HINT_JAVAEE_FETCH_GRAPH
	 * @see org.hibernate.jpa.LegacySpecHints#HINT_JAVAEE_LOAD_GRAPH
	 *
	 * @deprecated (since 6.0) Use {@link #getJakartaHintName} instead
	 */
	@Deprecated
	public String getJpaHintName() {
		return javaeeHintName;
	}

	public static GraphSemantic fromHintName(String hintName) {
		assert hintName != null;

		if ( FETCH.getJakartaHintName().equals( hintName ) || FETCH.getJpaHintName().equals( hintName ) ) {
			return FETCH;
		}

		if ( LOAD.getJakartaHintName().equalsIgnoreCase( hintName ) || LOAD.getJpaHintName().equalsIgnoreCase( hintName ) ) {
			return LOAD;
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"Unknown EntityGraph hint name - `%s`.  " +
								"Expecting `%s` or `%s` (or `%s` and `%s`).",
								hintName,
						FETCH.jakartaHintName,
						LOAD.jakartaHintName,
						FETCH.javaeeHintName,
						LOAD.javaeeHintName
				)
		);
	}

	/**
	 * @deprecated (since 6.0) Use {@link #fromHintName} instead
	 */
	@Deprecated
	public static GraphSemantic fromJpaHintName(String hintName) {
		return fromHintName( hintName );
	}
}
