/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm;

/**
 * Indicates violations of strict JPQL syntax while strict JPQL syntax checking was enabled.
 *
 * @author Steve Ebersole
 */
public class StrictJpaComplianceViolation extends SemanticException {
	public enum Type {
		IMPLICIT_SELECT( "implicit select clause" ),
		ALIASED_FETCH_JOIN( "aliased fetch join" ),
		UNMAPPED_POLYMORPHISM( "unmapped polymorphic reference" ),
		FUNCTION_CALL( "improper non-standard function call" ),
		HQL_COLLECTION_FUNCTION( "use of HQL collection functions (maxelement,minelement,maxindex,minindex)"),
		VALUE_FUNCTION_ON_NON_MAP( "use of value() function for non-Map type" ),
		RESERVED_WORD_USED_AS_ALIAS( "use of reserved word as alias (identification variable or result variable)" ),
		INDEXED_ELEMENT_REFERENCE( "use of HQL indexed element reference syntax" ),
		SUBQUERY_ORDER_BY( "use of ORDER BY clause in subquery" ),
		LIMIT_OFFSET_CLAUSE( "use of LIMIT/OFFSET clause" ),
		IDENTIFICATION_VARIABLE_NOT_DECLARED_IN_FROM_CLAUSE( "use of an alias not declared in the FROM clause" )
		;

		private final String description;

		Type(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	private final Type type;

	public StrictJpaComplianceViolation(Type type) {
		super( "Strict JPQL compliance was violated : " + type.description );
		this.type = type;
	}

	public StrictJpaComplianceViolation(String message, Type type) {
		super( message );
		this.type = type;
	}

	public Type getType() {
		return type;
	}
}
