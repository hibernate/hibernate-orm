/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Generalized description of a "type" that maps a domain type to one or more
 * JDBC types.  Defines a 2-phase process for writing values:
 *
 * 		* {@link #unresolve} - Transforms the domain representation of value of
 * 			this type into its "hydrated array" representation.  The exact outcome
 * 			of this depends on the nature of this type (basic, embedded, etc).
 * 			Named as a corollary to {@link Readable#resolveHydratedState}.
 * 		* {@link #dehydrate} - Transforms each value in the "hydrated" array
 * 			into a call to the supplied {@link JdbcValueCollector}
 *
 * @see Readable
 *
 * @author Steve Ebersole
 */
public interface Writeable {
	Predicate<StateArrayContributor> STANDARD_INSERT_INCLUSION_CHECK = StateArrayContributor::isInsertable;
	Predicate<StateArrayContributor> STANDARD_UPDATE_INCLUSION_CHECK = StateArrayContributor::isUpdatable;

	// todo (6.0) : the corollary to Writeable is really DomainResult(Assembler) / Initializer
	//		Readable should just go away

	// todo (6.0) : consider defining these contracts in terms of the inclusion checks rather than Clause
	// todo (6.0) : remove `#unresolve`
	// todo (6.0) : consider "recasting" `#visitJdbcTypes` as `#visitColumns` accepting `Consumer<Column>`
	//		originally went with SqlExpressableType because not every implementor was mapped to columns -
	//		originally BasicType, e.g., implemented this contract


	/**
	 * Contract used in dehydrating a value into its basic JDBC values.
	 *
	 * @see #dehydrate
	 */
	@FunctionalInterface
	interface JdbcValueCollector {
		void collect(Object jdbcValue, SqlExpressableType type, Column boundColumn);
	}


	/**
	 * Visit all of the included (per Clause) JDBC types defined by this Writeable type.
	 */
	default void visitJdbcTypes(
			Consumer<SqlExpressableType> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	/**
	 * Produce a multi-dimensional array of extracted simple value
	 */
	default Object unresolve(Object value, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	/**
	 * Produce a flattened array from dehydrated state
	 */
	default void dehydrate(
			Object value,
			JdbcValueCollector jdbcValueCollector,
			Clause clause,
			SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
