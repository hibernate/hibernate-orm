/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.select;

import org.hibernate.sql.exec.results.spi.QueryResultAssembler;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Represents a result value in the domain query results.
 * <p/>
 * Not the same as a result column in the JDBC ResultSet!  This contract
 * represents an individual result in the results of the query in terms of
 * domain objects.  A QueryResult will usually consume multiple result
 * columns.
 * <p/>
 * QueryResult is distinctly different from a {@link Fetch} and so modeled as
 * completely separate hierarchy.
 *
 * @see QueryResultScalar
 * @see QueryResultDynamicInstantiation
 * @see QueryResultEntity
 * @see QueryResultCollection
 * @see Fetch
 *
 * @author Steve Ebersole
 */
public interface QueryResult {
	/**
	 * The selection backing this QueryResult.
	 */
	Selection getSelection();

	/**
	 * The JavaTypeDescriptor describing the Java type of the described result
	 */
	JavaTypeDescriptor getJavaTypeDescriptor();

	/**
	 * The assembler for this result.  See the JavaDocs for QueryResultAssembler
	 * for details on its purpose,
	 */
	QueryResultAssembler getResultAssembler();
}
