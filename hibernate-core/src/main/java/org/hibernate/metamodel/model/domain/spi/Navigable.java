/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Models a "piece" of the application's domain model that can be navigated
 * as part of a query or the {@link NavigableVisitationStrategy} contract.
 *
 * @author Steve Ebersole
 */
public interface Navigable<T> extends DomainTypeDescriptor<T> {
	/**
	 * The NavigableContainer which contains this Navigable.
	 */
	NavigableContainer<?> getContainer();

	default EntityTypeDescriptor findFirstEntityDescriptor() {
		if ( this instanceof EntityValuedNavigable ) {
			return ( (EntityValuedNavigable) this ).getEntityDescriptor();
		}

		// do not read past collections
		final NavigableContainer<?> container = getContainer();
		if ( container != null ) {
			return container.findFirstEntityDescriptor();
		}

		throw new IllegalStateException( "Could not locate first entity descriptor" );
	}

	/**
	 * The role for this Navigable which is unique across all
	 * Navigables in the given TypeConfiguration.
	 */
	NavigableRole getNavigableRole();

	default String getNavigableName() {
		return getNavigableRole().getNavigableName();
	}

	default DomainTypeDescriptor<T> getNavigableType() {
		return this;
	}

	/**
	 * Visitation (walking) contract
	 *
	 * @param visitor The "visitor" responsibility in the Visitor pattern
	 */
	void visitNavigable(NavigableVisitationStrategy visitor);

	/**
	 * Finish initialization step.
	 *
	 * It's important to understand that the runtime creation process will call this
	 * method on the navigable until one of several conditions are met
	 *
	 * <ul>
	 *     <li>All navigables have returned true.</li>
	 *     <li>The navigable returned false and needs to wait for other dependencies.</li>
	 *     <li>The list of navigables have some missing or cyclic dependency</li>
	 * </ul>
	 *
	 * @param bootReference
	 * @param creationContext The context in which the Navigable is being created and
	 * finalized.
	 *
	 * @return true if initialization complete, false if not yet done.
	 */
	default boolean finishInitialization(
			Object bootReference,
			RuntimeModelCreationContext creationContext) {
		return true;
	}

	/**
	 * todo (6.0) : these calls should assume we are handling "implicit paths".  SqmFrom-paths are handled separately.
	 *
	 * todo (6.0) : would be really nice to have access to the "next token" as well
	 *
	 * todo (6.0) : is anything needed in place of `SqmNavigableContainerReference` argument?
	 * 		it ^^ should go away - the passed SqmFrom (should be SqmPath) is the LHS
	 */
	default SqmNavigableReference createSqmExpression(
			SqmFrom sourceSqmFrom,
			SqmNavigableContainerReference containerReference,
			SqmCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	default SqmNavigableReference createSqmExpression(
			SqmPath lhs,
			SqmCreationState creationState) {
		assert lhs.getReferencedNavigable() == getContainer();

		final NavigablePath navigablePath = lhs.getNavigablePath().append( getNavigableName() );

		if ( this instanceof BasicValuedNavigable ) {
			return (SqmNavigableReference) creationState.getProcessingStateStack().getCurrent().getPathRegistry().resolvePath(
					navigablePath,
					np -> new SqmBasicValuedSimplePath(
							creationState.generateUniqueIdentifier(),
							navigablePath,
							(BasicValuedNavigable) this,
							lhs,
							null
					)
			);
		}
		else if ( this instanceof EmbeddedValuedNavigable ) {
			return (SqmNavigableReference) creationState.getProcessingStateStack().getCurrent().getPathRegistry().resolvePath(
					navigablePath,
					np -> new SqmEmbeddedValuedSimplePath(
							creationState.generateUniqueIdentifier(),
							navigablePath,
							(EmbeddedValuedNavigable) this,
							lhs,
							null
					)
			);
		}
		else if ( this instanceof EntityValuedNavigable ) {
			return (SqmNavigableReference) creationState.getProcessingStateStack().getCurrent().getPathRegistry().resolvePath(
					navigablePath,
					np -> new SqmEntityValuedSimplePath(
							creationState.generateUniqueIdentifier(),
							navigablePath,
							(EntityValuedNavigable) this,
							lhs,
							null
					)
			);
		}
		else {
			throw new SemanticException( "Cannot de-reference path : " + lhs + " -> " + getNavigableName() );
		}
	}

	/**
	 * Obtain a loggable representation.
	 *
	 * @return The loggable representation of this reference
	 */
	String asLoggableText();

	/**
	 * Visit all of the columns to which this Navigable is mapped
	 */
	default void visitColumns(
			BiConsumer<SqlExpressableType,Column> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	/**
	 * Create a QueryResult for a specific reference to this Navigable.
	 *
	 * Ultimately this is called by the `QueryResultProducer#createDomainResult
	 * for the `NavigableReference` specialization of `QueryResultProducer`
	 */
	default DomainResult createDomainResult(
			NavigablePath navigablePath,
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	/**
	 * todo (6.0) : pass in Clause / Predicate<Column> ?
	 */
	default List<ColumnReference> resolveColumnReferences(
			ColumnReferenceQualifier qualifier,
			SqlAstCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
