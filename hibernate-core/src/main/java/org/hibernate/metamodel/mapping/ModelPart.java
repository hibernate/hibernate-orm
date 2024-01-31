/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.Objects;
import java.util.function.BiConsumer;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.type.descriptor.java.JavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Base descriptor, within the mapping model, for any part of the
 * application's domain model: an attribute, an entity identifier,
 * collection elements, and so on.
 *
 * @see DomainResultProducer
 * @see jakarta.persistence.metamodel.Bindable
 *
 * @author Steve Ebersole
 */
public interface ModelPart extends MappingModelExpressible {

	/**
	 * @asciidoc
	 *
	 * The path for this fetchable back to an entity in the domain model.  Acts as a unique
	 * identifier for individual parts.
	 *
	 * Some examples:
	 *
	 * For an entity, the role name is simply the entity name.
	 *
	 * For embeddable the role name is the path back to the root entity.  E.g. a Person's address
	 * would be a path `Person#address`.
	 *
	 * For a collection the path would be the same as the "collection role".  E.g. an Order's lineItems
	 * would be `Order#lineItems`.  This is the same as the historical `CollectionPersister#getRoleName`.
	 *
	 * For the (model)parts of a collection the role is either `{element}` or `{index}` depending.  E.g.
	 * `Order#lineItems.{element}`.  Attributes of the element or index type (embeddable or entity typed)
	 * would be based on this role.  E.g. `Order#lineItems.{element}.quantity`
	 *
	 * For an attribute of an embedded, the role would be relative to its "container".  E.g. `Person#address.city` or
	 * `Person#addresses.{element}.city`
	 *
	 * @apiNote Whereas {@link #getPartName()} is local to this part, NavigableRole can be a compound path
	 *
	 * @see #getPartName()
	 */
	NavigableRole getNavigableRole();

	/**
	 * The local part name, which is generally the unqualified role name
	 */
	String getPartName();

	/**
	 * The type for this part.
	 */
	MappingType getPartMappingType();

	/**
	 * The Java type for this part.  Generally equivalent to
	 * {@link MappingType#getMappedJavaType()} relative to
	 * {@link #getPartMappingType()}
	 */
	JavaType<?> getJavaType();

	/**
	 * Whether this model part describes something that physically
	 * exists in the domain model.
	 * <p/>
	 * For example, an entity's {@linkplain EntityDiscriminatorMapping discriminator}
	 * is part of the model, but is not a physical part of the domain model - there
	 * is no "discriminator attribute".
	 * <p/>
	 * Also indicates whether the part is castable to {@link VirtualModelPart}
	 */
	default boolean isVirtual() {
		return false;
	}

	default boolean isEntityIdentifierMapping() {
		return false;
	}

	boolean hasPartitionedSelectionMapping();

	/**
	 * Create a DomainResult for a specific reference to this ModelPart.
	 */
	<T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState);

	/**
	 * Apply SQL selections for a specific reference to this ModelPart outside the domain query's root select clause.
	 */
	void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState);

	/**
	 * Apply SQL selections for a specific reference to this ModelPart outside the domain query's root select clause.
	 */
	void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection,JdbcMapping> selectionConsumer);

	/**
	 * A short hand form of {@link #forEachSelectable(int, SelectableConsumer)}, that passes 0 as offset.
	 */
	default int forEachSelectable(SelectableConsumer consumer) {
		return forEachSelectable( 0, consumer );
	}

	/**
	 * Visits each selectable mapping with the selectable index offset by the given value.
	 * Returns the amount of jdbc types that have been visited.
	 */
	default int forEachSelectable(int offset, SelectableConsumer consumer) {
		return 0;
	}

	default AttributeMapping asAttributeMapping() {
		return null;
	}

	default EntityMappingType asEntityMappingType(){
		return null;
	}

	@Nullable default BasicValuedModelPart asBasicValuedModelPart() {
		return null;
	}

	/**
	 * A short hand form of {@link #breakDownJdbcValues(Object, int, Object, Object, JdbcValueBiConsumer, SharedSessionContractImplementor)},
	 * that passes 0 as offset and null for the two values {@code X} and {@code Y}.
	 */
	default int breakDownJdbcValues(
			Object domainValue,
			JdbcValueConsumer valueConsumer,
			SharedSessionContractImplementor session) {
		return breakDownJdbcValues( domainValue, 0, null, null, valueConsumer, session );
	}

	/**
	 * Breaks down the domain value to its constituent JDBC values.
	 *
	 * Think of it as breaking the multi-dimensional array into a visitable flat array.
	 * Additionally, it passes through the values {@code X} and {@code Y} to the consumer.
	 * Returns the amount of jdbc types that have been visited.
	 */
	<X, Y> int breakDownJdbcValues(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session);

	/**
	 * A short hand form of {@link #decompose(Object, int, Object, Object, JdbcValueBiConsumer, SharedSessionContractImplementor)},
	 * that passes 0 as offset and null for the two values {@code X} and {@code Y}.
	 */
	default int decompose(
			Object domainValue,
			JdbcValueConsumer valueConsumer,
			SharedSessionContractImplementor session) {
		return decompose( domainValue, 0, null, null, valueConsumer, session );
	}

	/**
	 * Similar to {@link #breakDownJdbcValues(Object, int, Object, Object, JdbcValueBiConsumer, SharedSessionContractImplementor)},
	 * but this method is supposed to be used for decomposing values for assignment expressions.
	 * Returns the amount of jdbc types that have been visited.
	 */
	default <X, Y> int decompose(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		return breakDownJdbcValues( domainValue, offset, x, y, valueConsumer, session );
	}

	EntityMappingType findContainingEntityMapping();

	default boolean areEqual(@Nullable Object one, @Nullable Object other, SharedSessionContractImplementor session) {
		// NOTE : deepEquals to account for arrays (compound natural-id)
		return Objects.deepEquals( one, other );
	}

	/**
	 * Functional interface for consuming the JDBC values.
	 */
	@FunctionalInterface
	interface JdbcValueConsumer extends JdbcValueBiConsumer<Object, Object> {
		@Override
		default void consume(int valueIndex, Object x, Object y, Object value, SelectableMapping jdbcValueMapping) {
			consume( valueIndex, value, jdbcValueMapping );
		}

		/**
		 * Consume a JDBC-level jdbcValue.  The JDBC jdbcMapping descriptor is also passed in
		 */
		void consume(int valueIndex, Object value, SelectableMapping jdbcValueMapping);
	}

	/**
	 * Functional interface for consuming the JDBC values, along with two values of type {@code X} and {@code Y}.
	 */
	@FunctionalInterface
	interface JdbcValueBiConsumer<X, Y> {
		/**
		 * Consume a JDBC-level jdbcValue.  The JDBC jdbcMapping descriptor is also passed in
		 */
		void consume(int valueIndex, X x, Y y, Object value, SelectableMapping jdbcValueMapping);
	}
}
