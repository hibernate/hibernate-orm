/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.result;

import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;

/**
 * Something that can produce a DomainResult as part of a SQM interpretation
 *
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
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
	@org.hibernate.SPI(org.hibernate.SPI.Role.SUPPLY)
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
