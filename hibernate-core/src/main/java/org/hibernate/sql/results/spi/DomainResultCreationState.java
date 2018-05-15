/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.results.internal.values.JdbcValues;

/**
 * @author Steve Ebersole
 */
public interface DomainResultCreationState {
	SqlExpressionResolver getSqlExpressionResolver();

	Stack<ColumnReferenceQualifier> getColumnReferenceQualifierStack();

	Stack<NavigableReference> getNavigableReferenceStack();

	SqlAliasBaseGenerator getSqlAliasBaseGenerator();

	boolean fetchAllAttributes();

	/**
	 * todo (6.0) : centralize the implementation of this
	 * 		most of the logic in the impls of this is identical.  variations (arguments) include:
	 * 				1) given a Fetchable, determine the FetchTiming and `selected`[1].  Tricky as functional
	 * 					interface because of the "composite return".
	 * 				2) given a Fetchable, determine the LockMode - currently not handled very well here; should consult `#getLockOptions`
	 * 						 - perhaps a functional interface accepting the FetchParent and Fetchable and returning the LockMode
	 *
	 * 			so something like:
	 * 				List<Fetch> visitFetches(
	 * 	 					FetchParent fetchParent,
	 * 	 					BiFunction<FetchParent,Fetchable,(FetchTiming,`selected`)> fetchStrategyResolver,
	 * 	 					BiFunction<FetchParent,Fetchable,LockMode> lockModeResolver)
	 *
	 * [1] `selected` refers to the named parameter in
	 * {@link Fetchable#generateFetch(org.hibernate.sql.results.spi.FetchParent, org.hibernate.engine.FetchTiming, boolean, org.hibernate.LockMode, java.lang.String, org.hibernate.sql.results.spi.DomainResultCreationState, org.hibernate.sql.results.spi.DomainResultCreationContext)}.
	 * For {@link org.hibernate.engine.FetchTiming#IMMEDIATE}, this boolean value indicates
	 * whether the values for the generated assembler/initializers are or should be available in
	 * the {@link JdbcValues} being processed.  For {@link org.hibernate.engine.FetchTiming#DELAYED} this
	 * parameter has no effect
	 */
	List<Fetch> visitFetches(FetchParent fetchParent);

	TableSpace getCurrentTableSpace();

	LockMode determineLockMode(String identificationVariable);

	// todo (6.0) : what else do we need to properly allow Fetch creation the ability to create/
	//		actually, this (^^) is not true
	//
	// fetches ought to include basic attributes too.  Handling "all attributes fetched"
	//		can be handled in 2 logical places:
	//			1) DomainResultCreationState#visitFetches - creates the sub-fetch graph
	//			2) The `Initializer` registered by each
}
