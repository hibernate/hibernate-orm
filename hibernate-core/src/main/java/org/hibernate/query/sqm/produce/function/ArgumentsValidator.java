/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function;

import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.tree.SqlAstNode;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public interface ArgumentsValidator {
	/**
	 * Perform validation that may be done using the {@link SqmTypedNode}
	 * tree and assigned Java types.
	 */
	void validate(List<? extends SqmTypedNode<?>> arguments, String functionName);

	default String getSignature() {
		return "( ... )";
	}

	/**
	 * Perform validation that requires the {@link SqlAstNode} tree and
	 * assigned JDBC types.
	 */
	default void validateSqlTypes(List<? extends SqlAstNode> arguments, String functionName) {}

	/**
	 * A mini-"type system" for HQL function parameters.
	 * <p>
	 * Note that typical database type systems have relatively few types,
	 * and lots of implicit type conversion between them. So we can be
	 * pretty forgiving here.
	 */
	enum ParameterType {
		/**
		 * @see org.hibernate.type.SqlTypes#isCharacterType(int)
		 */
		STRING,
		/**
		 * @see org.hibernate.type.SqlTypes#isNumericType(int)
		 */
		NUMERIC,
		/**
		 * @see org.hibernate.type.SqlTypes#isIntegral(int)
		 */
		INTEGER,
		/**
		 * @see org.hibernate.type.SqlTypes#isTemporalType(int)
		 */
		TEMPORAL,
		/**
		 * @see org.hibernate.type.SqlTypes#hasDatePart(int)
		 */
		DATE,
		/**
		 * @see org.hibernate.type.SqlTypes#hasTimePart(int)
		 */
		TIME,
		/**
		 * Indicates that the argument should be of type
		 * {@link org.hibernate.type.SqlTypes#BOOLEAN} or
		 * a logical expression (predicate)
		 */
		BOOLEAN,
		/**
		 * Indicates a parameter that accepts any type
		 */
		ANY
	}
}
