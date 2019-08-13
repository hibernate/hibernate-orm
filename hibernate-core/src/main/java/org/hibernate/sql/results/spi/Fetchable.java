/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.model.mapping.spi.ValueMapping;

/**
 * @author Steve Ebersole
 */
public interface Fetchable extends ValueMapping {
	String getFetchableName();

	FetchStrategy getMappedFetchStrategy();

	// todo (6.0) : all we need here is (1) FetchTiming and (2) whether the values are available in the current JdbcValuesSource
	//		Having to instantiate new FetchStrategy potentially multiple times
	// 		per Fetch generation is performance drain.  Would be better to
	// 		simply pass these 2 pieces of information

	Fetch generateFetch(
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState);

	default boolean isCircular(FetchParent fetchParent){
		return false;
	}

}
