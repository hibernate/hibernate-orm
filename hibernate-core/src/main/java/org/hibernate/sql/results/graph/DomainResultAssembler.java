/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import org.hibernate.Incubating;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Responsible for "assembling" a result for inclusion in the domain query
 * result.  "Assembling" the result basically means building the result object
 * (whatever that means for a specific result type) and returning it for
 * injection into the result "row" currently being processed
 *
 * @author Steve Ebersole
 */
@Incubating
public interface DomainResultAssembler<J> {
	/**
	 * The main "assembly" contract.  Assemble the result and return it.
	 */
	J assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options);

	/**
	 * Convenience form of {@link #assemble(RowProcessingState, JdbcValuesSourceProcessingOptions)}
	 */
	default J assemble(RowProcessingState rowProcessingState) {
		return assemble( rowProcessingState, rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions() );
	}

	/**
	 * The JavaType describing the Java type that this assembler
	 * assembles.
	 */
	JavaType<J> getAssembledJavaType();

	/**
	 * This method is used to resolve the assembler's state, i.e. reading the result values,
	 * with some performance optimization when we don't need the result object itself
	 */
	default void resolveState(RowProcessingState rowProcessingState) {
		assemble( rowProcessingState );
	}
}
