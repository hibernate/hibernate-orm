/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi;

import java.util.List;

import org.hibernate.query.sqm.tree.from.SqmFromClause;

/**
 * @author Steve Ebersole
 */
public interface QuerySpecProcessingState extends RootSqmNavigableReferenceLocator {
	SqmCreationContext getSqmCreationContext();

	AliasRegistry getAliasRegistry();

	/**
	 * @apiNote The returned from-clause may still be "in-flight"
	 */
	SqmFromClause getFromClause();


	QuerySpecProcessingState getContainingQueryState();
	List<QuerySpecProcessingState> getSubQueryStateList();
}
