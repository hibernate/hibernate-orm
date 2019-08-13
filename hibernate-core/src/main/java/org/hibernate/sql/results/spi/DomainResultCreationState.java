/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.query.sqm.sql.SqlAliasBaseManager;
import org.hibernate.query.sqm.sql.SqlAstCreationState;

/**
 * @author Steve Ebersole
 */
public interface DomainResultCreationState {
	SqlAstCreationState getSqlAstCreationState();

	default SqlAliasBaseManager getSqlAliasBaseManager() {
		return (SqlAliasBaseManager) getSqlAstCreationState().getSqlAliasBaseGenerator();
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
	 *
	 * todo (6.0) : wrt the "trickiness" of `selected[1]`, that may no longer be an issue given how TableGroups
	 * 		are built/accessed.  Comes down to how we'd know whether to join fetch or select fetch.  Simply pass
	 * 		along FetchStyle?
	 *
	 *
	 */
	List<Fetch> visitFetches(FetchParent fetchParent);
}
