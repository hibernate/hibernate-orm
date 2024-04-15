/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph;

/**
 * JPA defines 2 distinct semantics for applying an EntityGraph.  This
 * enumeration captures those 2 semantics.
 *
 * @author Steve Ebersole
 */
public enum GraphSemantic {
	/**
	 * Indicates a "fetch graph" EntityGraph.  Attributes explicitly specified
	 * as AttributeNodes are treated as FetchType.EAGER (via join fetch or
	 * subsequent select). Attributes that are not specified are treated as
	 * FetchType.LAZY invariably.
	 */
	FETCH( "javax.persistence.fetchgraph", "jakarta.persistence.fetchgraph" ),

	/**
	 * Indicates a "load graph" EntityGraph.  Attributes explicitly specified
	 * as AttributeNodes are treated as FetchType.EAGER (via join fetch or
	 * subsequent select).  Attributes that are not specified are treated as
	 * FetchType.LAZY or FetchType.EAGER depending on the attribute's definition
	 * in metadata.
	 */
	LOAD( "javax.persistence.loadgraph", "jakarta.persistence.loadgraph" );

	private final String jpaHintName;
	private final String jakartaJpaHintName;

	GraphSemantic(String jpaHintName, String jakartaJpaHintName) {
		this.jpaHintName = jpaHintName;
		this.jakartaJpaHintName = jakartaJpaHintName;
	}

	public String getJpaHintName() {
		return jpaHintName;
	}

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
