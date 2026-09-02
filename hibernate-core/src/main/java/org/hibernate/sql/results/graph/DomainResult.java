/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph;

import org.hibernate.Incubating;

/**
 * Represents a result value in the domain query results.  Acts as the producer for the
 * {@link DomainResultAssembler} for this result as well as any {@link Initializer} instances needed
 *
 * Not the same as a result column in the JDBC ResultSet!  This contract represents an individual
 * domain-model-level query result.  A DomainResult will usually consume multiple JDBC result columns.
 *
 * DomainResult is distinctly different from a {@link Fetch} and so modeled as completely separate hierarchy.
 *
 * @see Fetch
 * @see org.hibernate.sql.ast.spi.result.DomainResultProducer#createDomainResult(String, DomainResultCreationState)
 * @see org.hibernate.metamodel.mapping.ModelPart#createDomainResult(org.hibernate.spi.NavigablePath, org.hibernate.sql.ast.spi.query.from.TableGroup, String, DomainResultCreationState)
 * @see org.hibernate.query.results.spi.ResultBuilder#buildResult(org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata, int, DomainResultCreationState)
 *
 * @author Steve Ebersole
 */
@Incubating
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT, org.hibernate.SPI.Role.SUPPLY })
public interface DomainResult<J> extends DomainResultGraphNode {
	/**
	 * The result-variable (alias) associated with this result.
	 */
	String getResultVariable();

	/**
	 * Create an assembler (and any initializers) for this result.
	 *
	 * @see DomainResultAssembler
	 */
	@org.hibernate.SPI(org.hibernate.SPI.Role.SUPPLY)
	DomainResultAssembler<J> createResultAssembler(
			InitializerParent<?> parent,
			AssemblerCreationState creationState);
}
