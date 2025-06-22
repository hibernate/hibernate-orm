/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.Incubating;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;

/**
 * Contract for things at the domain mapping level that can be bound
 * into a JDBC {@link java.sql.PreparedStatement}.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface Bindable extends JdbcMappingContainer {

	/**
	 * The number of JDBC mappings
	 */
	@Override
	default int getJdbcTypeCount() {
		return forEachJdbcType( (index, jdbcMapping) -> {} );
	}

	/**
	 * Visit each of JdbcMapping
	 *
	 * @apiNote Same as {@link #forEachJdbcType(int, IndexedConsumer)} starting from `0`
	 */
	@Override
	default int forEachJdbcType(IndexedConsumer<JdbcMapping> action) {
		return forEachJdbcType( 0, action );
	}

	/**
	 * Breaks down a value of {@code J} into its simple pieces.  E.g., an embedded
	 * value gets broken down into an array of its attribute state; a basic
	 * value converts to itself; etc.
	 * <p>
	 * Generally speaking, this is the form in which entity state is kept relative to a
	 * Session via {@code EntityEntry}.
	 * <p>
	 * <pre>
	 * &#64;Entity class Person {
	 *     &#64;Id Integer id;
	 *     &#64;Embedded Name name;
	 *     int age;
	 * }
	 *
	 * &#64;Embeddable class Name {
	 *     String familiarName;
	 *     String familyName;
	 * }
	 * </pre>
	 * <p>
	 * At the top level, we would want to disassemble a {@code Person} value, so we'd
	 * ask the {@code Bindable} for the {@code Person} entity to disassemble.  Given a
	 * {@code Person} value:
	 * <p>
	 * <pre>
	 * Person( id=1, name=Name( 'Steve', 'Ebersole' ), 28 )
	 * </pre>
	 * <p>
	 * this disassemble would result in a multidimensional array:
	 * <p>
	 * <pre>
	 * [ ["Steve", "Ebersole"], 28 ]
	 * </pre>
	 * <p>
	 * Note that the identifier is not part of this disassembled state.  Note also
	 * how the embedded value results in a sub-array.
	 *
	 * @see org.hibernate.engine.spi.EntityEntry
	 * <p>
	 */
	Object disassemble(Object value, SharedSessionContractImplementor session);

	/**
	 * Add to the MutableCacheKey the values obtained disassembling the value and the hasCode generated from
	 * the disassembled value.
	 *
	 * @param cacheKey the MutableCacheKey used to add the disassembled value and the hashCode
	 * @param value the value to disassemble
	 * @param session the SharedSessionContractImplementor
	 */
	void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session);

	/**
	 * Visit each constituent JDBC value over the result from {@link #disassemble}.
	 * <p>
	 * Given the example in {@link #disassemble}, this results in the consumer being
	 * called for each simple value.  E.g.:
	 * <p>
	 * <pre>
	 * consumer.consume( "Steve" );
	 * consumer.consume( "Ebersole" );
	 * consumer.consume( 28 );
	 * </pre>
	 * <p>
	 * Think of it as breaking the multidimensional array into a visitable flat array.
	 * Additionally, it passes through the values {@code X} and {@code Y} to the consumer.
	 */
	default <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		return forEachDisassembledJdbcValue( value, 0, x, y, valuesConsumer, session );
	}

	/**
	 * Like {@link #forEachDisassembledJdbcValue(Object, Object, Object, JdbcValuesBiConsumer, SharedSessionContractImplementor)},
	 * but additionally receives an offset by which the selectionIndex is incremented when calling {@link JdbcValuesBiConsumer#consume(int, Object, Object, Object, JdbcMapping)}.
	 */
	<X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session);

	/**
	 * A short hand form of {@link #forEachDisassembledJdbcValue(Object, Object, Object, JdbcValuesBiConsumer, SharedSessionContractImplementor)},
	 * that passes null for the two values {@code X} and {@code Y}.
	 */
	default int forEachDisassembledJdbcValue(
			Object value,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		return forEachDisassembledJdbcValue( value, null, null, valuesConsumer, session );
	}

	/**
	 * A short hand form of {@link #forEachDisassembledJdbcValue(Object, int, Object, Object, JdbcValuesBiConsumer, SharedSessionContractImplementor)},
	 * that passes null for the two values {@code X} and {@code Y} .
	 */
	default int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		return forEachDisassembledJdbcValue( value, offset, null, null, valuesConsumer, session );
	}

	/**
	 * Visit each constituent JDBC value extracted from the entity instance itself.
	 *
	 * Short-hand form of calling {@link #disassemble} and piping its result to
	 * {@link #forEachDisassembledJdbcValue(Object, JdbcValuesConsumer, SharedSessionContractImplementor)}
	 */
	default <X, Y> int forEachJdbcValue(
			Object value,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		return forEachJdbcValue( value, 0, x, y, valuesConsumer, session );
	}

	/**
	 * Visit each constituent JDBC value extracted from the entity instance itself.
	 *
	 * Short-hand form of calling {@link #disassemble} and piping its result to
	 * {@link #forEachDisassembledJdbcValue(Object, int, JdbcValuesConsumer, SharedSessionContractImplementor)}
	 */
	default <X, Y> int forEachJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		return forEachDisassembledJdbcValue( disassemble( value, session ), offset, x, y, valuesConsumer, session );
	}

	/**
	 * A short hand form of {@link #forEachJdbcValue(Object, Object, Object, JdbcValuesBiConsumer, SharedSessionContractImplementor)},
	 * that passes null for the two values {@code X} and {@code Y}.
	 */
	default int forEachJdbcValue(
			Object value,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		return forEachJdbcValue( value, null, null, valuesConsumer, session );
	}

	/**
	 * A short hand form of {@link #forEachJdbcValue(Object, int, Object, Object, JdbcValuesBiConsumer, SharedSessionContractImplementor)},
	 * that passes null for the two values {@code X} and {@code Y}.
	 */
	default int forEachJdbcValue(
			Object value,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		return forEachJdbcValue( value, offset, null, null, valuesConsumer, session );
	}

	/**
	 * Functional interface for consuming the JDBC values.
	 */
	@FunctionalInterface
	interface JdbcValuesConsumer extends JdbcValuesBiConsumer<Object, Object> {
		@Override
		default void consume(int valueIndex, Object o, Object o2, Object jdbcValue, JdbcMapping jdbcMapping) {
			consume( valueIndex, jdbcValue, jdbcMapping );
		}

		/**
		 * Consume a JDBC-level jdbcValue.  The JDBC jdbcMapping descriptor is also passed in
		 */
		void consume(int valueIndex, Object jdbcValue, JdbcMapping jdbcMapping);
	}

	/**
	 * Functional interface for consuming the JDBC values, along with two values of type {@code X} and {@code Y}.
	 */
	@FunctionalInterface
	interface JdbcValuesBiConsumer<X, Y> {
		/**
		 * Consume a JDBC-level jdbcValue.  The JDBC jdbcMapping descriptor is also passed in
		 */
		void consume(int valueIndex, X x, Y y, Object jdbcValue, JdbcMapping jdbcMapping);
	}
}
