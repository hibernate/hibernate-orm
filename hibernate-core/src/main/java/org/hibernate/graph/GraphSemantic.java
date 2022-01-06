/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph;

/**
 * JPA specifies two distinct ways to apply an {@link jakarta.persistence.EntityGraph},
 * as a {@link #FETCH "fetch graph"}, or as a {@link #LOAD "load graph"}.
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
	FETCH( "javax.persistence.fetchgraph", "jakarta.persistence.fetchgraph" ),

	/**
	 * Indicates that an {@link jakarta.persistence.EntityGraph} should be interpreted as a JPA "load graph".
	 * <ul>
	 * <li>Attributes explicitly specified using an {@link org.hibernate.graph.AttributeNode}s are treated as
	 * {@link jakarta.persistence.FetchType#EAGER} and fetched via a join or subsequent select.
	 * <li>Attributes not explicitly specified are treated as {@code FetchType.LAZY} or {@code FetchType.EAGER}
	 * depending on the mapping of the attribute, instead of forcing {@code FetchType.LAZY}.
	 * </ul>
	 */
	LOAD( "javax.persistence.loadgraph", "jakarta.persistence.loadgraph" );

	private final String jpaHintName;
	private final String jakartaJpaHintName;

	GraphSemantic(String jpaHintName, String jakartaJpaHintName) {
		this.jpaHintName = jpaHintName;
		this.jakartaJpaHintName = jakartaJpaHintName;
	}

	/**
	 * The hint name that should be used with JPA.
	 *
	 * @see jakarta.persistence.Query#setHint(String, Object)
	 */
	public String getJpaHintName() {
		return jpaHintName;
	}

	/**
	 * The hint name that should be used with Jakarta Persistence.
	 *
	 * @see jakarta.persistence.Query#setHint(String, Object)
	 */
	public String getJakartaJpaHintName() {
		return jakartaJpaHintName;
	}

	public static GraphSemantic fromJpaHintName(String hintName) {
		assert hintName != null;

		if ( FETCH.getJpaHintName().equals( hintName ) || FETCH.getJakartaJpaHintName().equals( hintName ) ) {
			return FETCH;
		}

		if ( LOAD.getJpaHintName().equalsIgnoreCase( hintName ) || LOAD.getJakartaJpaHintName().equalsIgnoreCase( hintName ) ) {
			return LOAD;
		}

		throw new IllegalArgumentException(
				"Unknown EntityGraph hint name [" + hintName + "]; " +
						"expecting `" + FETCH.jpaHintName + "` or `" + LOAD.jpaHintName + "`."
		);
	}
}
