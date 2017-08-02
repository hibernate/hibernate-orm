/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.results.spi;

import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;

/**
 * Represents a result value in the domain query results.  Acts as the
 * producer for the {@link QueryResultAssembler} for this result as well
 * as any {@link Initializer} instances needed
 * <p/>
 * Not the same as a result column in the JDBC ResultSet!  This contract
 * represents an individual domain-model-level query result.  A QueryResult
 * will usually consume multiple JDBC result columns.
 * <p/>
 * QueryResult is distinctly different from a {@link Fetch} and so modeled as
 * completely separate hierarchy.
 *
 * @see QueryResultScalar
 * @see QueryResultDynamicInstantiation
 * @see QueryResultEntity
 * @see QueryResultCollection
 * @see QueryResultComposite
 * @see Fetch
 *
 * @author Steve Ebersole
 */
public interface QueryResult {
	/**
	 * The result-variable (alias) associated with this result.
	 */
	String getResultVariable();

	/**
	 * Gets descriptor describing the type of the return.
	 *
	 * @return The type of the scalar return.
	 */
	ExpressableType getType();

	/**
	 * Register any `Initializer`s needed by this result object, potentially
	 * including `Initializer`s for fetches.
	 */
	void registerInitializers(InitializerCollector collector);

	/**
	 * The assembler for this result.  See the JavaDocs for QueryResultAssembler
	 * for details on its purpose,
	 */
	QueryResultAssembler getResultAssembler();
}
