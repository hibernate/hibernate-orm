/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.ast.Clause;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Contract for things at the domain/mapping level that can be bound into a JDBC
 * query.
 *
 * Notice that there may be more than one JDBC parameter involved here - an embedded value, e.g.
 *
 * @author Steve Ebersole
 */
public interface Bindable {
	default int getJdbcTypeCount(TypeConfiguration typeConfiguration) {
		final AtomicInteger value = new AtomicInteger( 0 );
		visitJdbcTypes(
				sqlExpressableType -> value.incrementAndGet(),
				Clause.IRRELEVANT,
				typeConfiguration
		);

		return value.get();
	}

	default List<JdbcMapping> getJdbcMappings(TypeConfiguration typeConfiguration) {
		final List<JdbcMapping> results = new ArrayList<>();
		visitJdbcTypes(
				results::add,
				Clause.IRRELEVANT,
				typeConfiguration
		);
		return results;
	}

	// todo (6.0) : why did I do 2 forms of JDBC-type visiting?  in-flight change?

	/**
	 * Visit all of the SqlExpressableTypes associated with this this Bindable.
	 * <p>
	 * Used during cacheable SQL AST creation.
	 */
	default void visitJdbcTypes(
			Consumer<JdbcMapping> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		throw new NotYetImplementedFor6Exception( getClass() );
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
	default void visitDisassembledJdbcValues(
			Object value,
			Clause clause,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	/**
	 * Visit each constituent JDBC value extracted from the entity instance itself.
	 *
	 * Short-hand form of calling {@link #disassemble} and piping its result to
	 * {@link #visitDisassembledJdbcValues}
	 *
	 * todo (6.0) : Would this would ever be used?
	 */
	default void visitJdbcValues(
			Object value,
			Clause clause,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		visitDisassembledJdbcValues( disassemble( value, session ), clause, valuesConsumer, session );
	}


	/**
	 * Functional interface for consuming the JDBC values.  Essentially a {@link java.util.function.BiConsumer}
	 */
	@FunctionalInterface
	interface JdbcValuesConsumer {
		/**
		 * Consume a JDBC-level value.  The JDBC type descriptor is also passed in
		 */
		void consume(Object value, JdbcMapping type);
	}
}
