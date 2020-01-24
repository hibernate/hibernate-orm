/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;

/**
 * @author Steve Ebersole
 */
public interface Fetchable extends ModelPart {
	String getFetchableName();

	FetchStrategy getMappedFetchStrategy();

	// todo (6.0) : all we need here is (1) FetchTiming and (2) whether the values are available in the current JdbcValuesSource
	//		Having to instantiate new FetchStrategy potentially multiple times
	// 		per Fetch generation is performance drain.  Would be better to
	// 		simply pass these 2 pieces of information

	/**
	 * For an association, this would return the foreign-key's "referring columns".  Would target
	 * the columns defined by {@link EntityValuedModelPart#getIdentifyingColumnExpressions}
	 */
	String[] getIdentifyingColumnExpressions();

	default Fetch resolveCircularFetch(
			NavigablePath fetchablePath,
			FetchParent fetchParent,
			SqlAstProcessingState creationState) {
		return null;
	}

	Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState);
}
