/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
 *
 * @author Steve Ebersole
 */
@Incubating
public interface DomainResult<J> extends DomainResultGraphNode {
	/**
	 * The result-variable (alias) associated with this result.
	 */
	String getResultVariable();

	/**
	 * Create an assembler (and any initializers) for this result.
	 */
	DomainResultAssembler<J> createResultAssembler(
			InitializerParent<?> parent,
			AssemblerCreationState creationState);
}
