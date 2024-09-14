/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;

/**
 * Something that can produce a DomainResult as part of a SQM interpretation
 *
 * @author Steve Ebersole
 */
public interface DomainResultProducer<T> {

	// this has to be designed as a bridge, but more geared toward the SQL

	/*
	 * select p.name, p2.name from Person p, Person p2
	 *
	 * SqmPathSource (SqmExpressible) (unmapped)
	 *
	 * DomainType
	 * SimpleDomainType
	 * ...
	 *
	 * MappingType
	 *
	 *
	 *
	 * ValueMapping (mapped)
	 *
	 *
	 * ModelPartContainer personMapping = ...;
	 * personMapping.getValueMapping( "name" );
	 */

	/**
	 * Produce the domain query
	 */
	DomainResult<T> createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState);

	/**
	 * Used when this producer is a selection in a sub-query.  The
	 * DomainResult is only needed for root query of a SELECT statement.
	 *
	 * This default impl assumes this producer is a true (Sql)Expression
	 */
	void applySqlSelections(DomainResultCreationState creationState);
}
