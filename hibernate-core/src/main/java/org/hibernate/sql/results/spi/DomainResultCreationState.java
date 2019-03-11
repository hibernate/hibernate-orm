/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.internal.util.collections.EmptyStack;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.SqlAstCreationLogger;
import org.hibernate.sql.ast.produce.SqlTreeException;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.results.internal.values.JdbcValues;

/**
 * @author Steve Ebersole
 */
public interface DomainResultCreationState {
	SqlAstCreationState getSqlAstCreationState();

	default SqlExpressionResolver getSqlExpressionResolver() {
		return getSqlAstCreationState().getSqlExpressionResolver();
	}

	default SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
		return getSqlAstCreationState().getSqlAliasBaseGenerator();
	}

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
	 * {@link Fetchable#generateFetch(FetchParent, org.hibernate.engine.FetchTiming, boolean, LockMode, String, DomainResultCreationState)}.
	 * For {@link org.hibernate.engine.FetchTiming#IMMEDIATE}, this boolean value indicates
	 * whether the values for the generated assembler/initializers are or should be available in
	 * the {@link JdbcValues} being processed.  For {@link org.hibernate.engine.FetchTiming#DELAYED} this
	 * parameter has no effect
	 */
	List<Fetch> visitFetches(FetchParent fetchParent);


	// todo (6.0) : better to define FromClauseAccess on SqlAstCreationState?

	FromClauseAccess getFromClauseAccess();

	interface FromClauseAccess {
		default TableGroup findTableGroup(NavigablePath navigablePath) {
			throw new NotYetImplementedFor6Exception();
		}

		default TableGroup getTableGroup(NavigablePath navigablePath) {
			final TableGroup tableGroup = findTableGroup( navigablePath );
			if ( tableGroup == null ) {
				throw new SqlTreeException( "Could not locate TableGroup - " + navigablePath );
			}
			return tableGroup;
		}

		default TableGroup locateTableGroup(NavigablePath navigablePath) {
			NavigablePath navigablePathToCheck = navigablePath;

			while ( navigablePathToCheck != null ) {
				final TableGroup tableGroup = findTableGroup( navigablePathToCheck );
				if ( tableGroup != null ) {
					return tableGroup;
				}

				navigablePathToCheck = navigablePathToCheck.getParent();
			}

			return null;

		}

		default void registerTableGroup(NavigablePath navigablePath, TableGroup tableGroup) {
			throw new NotYetImplementedFor6Exception();
		}

		default TableGroup resolveTableGroup(
				NavigablePath navigablePath,
				Function<NavigablePath, TableGroup> creator) {
			TableGroup tableGroup = findTableGroup( navigablePath );
			if ( tableGroup == null ) {
				tableGroup = creator.apply( navigablePath );
				registerTableGroup( navigablePath, tableGroup );
			}
			return tableGroup;
		}
	}

	class SimpleFromClauseAccessImpl implements FromClauseAccess {
		private final Map<NavigablePath, TableGroup> tableGroupMap = new HashMap<>();

		@Override
		public TableGroup findTableGroup(NavigablePath navigablePath) {
			return tableGroupMap.get( navigablePath );
		}

		@Override
		public void registerTableGroup(NavigablePath navigablePath, TableGroup tableGroup) {
			final TableGroup previous = tableGroupMap.put( navigablePath, tableGroup );
			if ( previous != null ) {
				SqlAstCreationLogger.LOGGER.debugf(
						"Registration of TableGroup [%s] for NavigablePath [%s] overrode previous registration : %s",
						tableGroup,
						navigablePath,
						previous
				);
			}
		}
	}


	// Things to go away
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	boolean fetchAllAttributes();

	LockMode determineLockMode(String identificationVariable);
}
