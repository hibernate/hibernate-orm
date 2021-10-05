/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.sql.ast.Clause;

/**
 * Contract for things at the domain/mapping level that can be bound into a JDBC
 * query.
 *
 * Notice that there may be more than one JDBC parameter involved here - an embedded value, e.g.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface Bindable extends JdbcMappingContainer {
	/*
	 * todo (6.0) : much of this contract uses Clause which (1) kludgy and (2) not always necessary
	 *  		- e.g. see the note below wrt "2 forms of JDBC-type visiting"
	 *
	 * Instead, in keeping with the general shift away from the getter paradigm to a more functional (Consumer,
	 * Function, etc) paradigm, I propose something more like:
	 *
	 * interface Bindable {
	 * 		void apply(UpdateStatement sqlAst, ..., SqlAstCreationState creationState);
	 * 		void apply(DeleteStatement sqlAst, ..., SqlAstCreationState creationState);
	 *
	 * 		Expression toSqlAst(..., SqlAstCreationState creationState);
	 *
	 * 		// plus the `DomainResult`, `Fetch` (via `DomainResultProducer` and `Fetchable`)
	 * 		// handling most impls already provide
	 * }
	 */

	/**
	 * The number of JDBC mappings
	 */
	default int getJdbcTypeCount() {
		return forEachJdbcType( (index, jdbcMapping) -> {} );
	}

	/**
	 * The list of JDBC mappings
	 */
	default List<JdbcMapping> getJdbcMappings() {
		final List<JdbcMapping> results = new ArrayList<>();
		forEachJdbcType( (index, jdbcMapping) -> results.add( jdbcMapping ) );
		return results;
	}

	// todo (6.0) : why did I do 2 forms of JDBC-type visiting?  in-flight change?

	/**
	 * Visit each of JdbcMapping
	 *
	 * @apiNote Same as {@link #forEachJdbcType(int, IndexedConsumer)} starting from `0`
	 */
	default int forEachJdbcType(IndexedConsumer<JdbcMapping> action) {
		return forEachJdbcType( 0, action );
	}

	/**
	 * @asciidoc
	 *
	 * Breaks down a value of `J` into its simple pieces.  E.g., an embedded
	 * value gets broken down into an array of its attribute state; a basic
	 * value converts to itself; etc.
	 * <p>
	 * Generally speaking, this is the form in which entity state is kept relative to a
	 * Session via `EntityEntry`.
	 *
	 * @see org.hibernate.engine.spi.EntityEntry
	 *
	 * As an example, consider the following domain model:
	 *
	 * ````
	 * @Entity
	 * class Person {
	 * 		@Id Integer id;
	 * 		@Embedded Name name;
	 * 		int age;
	 * }
	 *
	 * @Embeddable
	 * class Name {
	 *     String familiarName;
	 *     String familyName;
	 * }
	 * ````
	 *
	 * At the top-level, we would want to disassemble a `Person` value so we'd ask the
	 * `Bindable` for the `Person` entity to disassemble.  Given a Person value:
	 *
	 * ````
	 * Person( id=1, name=Name( 'Steve', 'Ebersole' ), 28 )
	 * ````
	 *
	 * this disassemble would result in a multi-dimensional array:
	 *
	 * ````
	 * [ ["Steve", "Ebersole"], 28 ]
	 * ````
	 *
	 * Note that the identifier is not part of this disassembled state.  Note also
	 * how the embedded value results in a sub-array.
	 */
	default Object disassemble(Object value, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	/**
	 * @asciidoc
	 *
	 * Visit each constituent JDBC value over the result from {@link #disassemble}.
	 *
	 * Given the example in {@link #disassemble}, this results in the consumer being
	 * called for each simple value.  E.g.:
	 *
	 * ````
	 * consumer.consume( "Steve" );
	 * consumer.consume( "Ebersole" );
	 * consumer.consume( 28 );
	 * ````
	 *
	 * Think of it as breaking the multi-dimensional array into a visitable flat array
	 */
	default int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		return forEachDisassembledJdbcValue( value, clause, 0, valuesConsumer, session );
	}

	default int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	/**
	 * Visit each constituent JDBC value extracted from the entity instance itself.
	 *
	 * Short-hand form of calling {@link #disassemble} and piping its result to
	 * {@link #forEachDisassembledJdbcValue}
	 *
	 * todo (6.0) : Would this would ever be used?
	 */
	default int forEachJdbcValue(
			Object value,
			Clause clause,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		return forEachJdbcValue( value, clause, 0, valuesConsumer, session );
	}

	default int forEachJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		return forEachDisassembledJdbcValue( disassemble( value, session ), clause, offset, valuesConsumer, session );
	}


	/**
	 * Functional interface for consuming the JDBC values.  Essentially a {@link java.util.function.BiConsumer}
	 */
	@FunctionalInterface
	interface JdbcValuesConsumer {
		/**
		 * Consume a JDBC-level jdbcValue.  The JDBC jdbcMapping descriptor is also passed in
		 */
		void consume(int selectionIndex, Object jdbcValue, JdbcMapping jdbcMapping);
	}
}
