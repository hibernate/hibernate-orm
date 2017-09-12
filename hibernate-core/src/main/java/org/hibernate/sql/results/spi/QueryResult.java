/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

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
 * @see ScalarQueryResult
 * @see DynamicInstantiationQueryResult
 * @see EntityQueryResult
 * @see PluralAttributeQueryResult
 * @see CompositeQueryResult
 * @see Fetch
 *
 * @author Steve Ebersole
 */
public interface QueryResult extends ResultSetMappingNode {
	/**
	 * The result-variable (alias) associated with this result.
	 */
	String getResultVariable();

	default JavaTypeDescriptor getJavaTypeDescriptor() {
		return getResultAssembler().getJavaTypeDescriptor();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// todo (6.0) : Consider a single "resolution" phase for QueryResult
	// 		Ultimately the best solution seems to be to allow the QueryResult to
	//		"resolve" itself at once in terms of building the proper
	//		Initializers and QueryResultAssembler.
	//
	// 		Potentially something like:
	/*
	ResolveResult resolve(QueryResultResolutionContext context, ???);

	interface QueryResultResolutionContext {
		SqlExpressionResolver getSqlExpressionResolver();
	}

	interface ResolveResult {
		void registerInitializers(InitializerCollector collector);
		QueryResultAssembler getResultAssembler();
	}
	*/

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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
