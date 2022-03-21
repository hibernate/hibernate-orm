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
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Describes a mapping related to any part of the app's domain model - e.g.
 * an attribute, an entity identifier, collection elements, etc
 *
 * @see DomainResultProducer
 * @see jakarta.persistence.metamodel.Bindable
 *
 * @author Steve Ebersole
 */
public interface ModelPart extends MappingModelExpressible {
	MappingType getPartMappingType();

	JavaType<?> getJavaType();

	String getPartName();

	/**
	 * @asciidoc
	 *
	 * The path for this fetchable back to an entity in the domain model.
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

	default int forEachSelectable(SelectableConsumer consumer) {
		return forEachSelectable( 0, consumer );
	}

	default int forEachSelectable(int offset, SelectableConsumer consumer) {
		return 0;
	}

	@FunctionalInterface
	interface JdbcValueConsumer {
		void consume(Object value, SelectableMapping jdbcValueMapping);
	}

	void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session);

	EntityMappingType findContainingEntityMapping();

	default boolean areEqual(Object one, Object other, SharedSessionContractImplementor session) {
		// NOTE : deepEquals to account for arrays (compound natural-id)
		return Objects.deepEquals( one, other );
	}
}
