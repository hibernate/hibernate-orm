/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.mapping.spi;

import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.persister.SqlExpressableType;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Contract for things at the domain/mapping level that can be extracted from a JDBC result
 *
 * Really, reading/loading stuff is defined via {@link DomainResult} and
 * {@link org.hibernate.sql.results.spi.Initializer}.  This contract simply works as a sort
 * of extended `DomainResultProducer` specifically for mapped-parts of a domain model
 *
 * @author Steve Ebersole
 */
public interface Readable {

	/**
	 * Visit all of the SqlExpressableTypes associated with this this Readable.
	 *
	 * Used during cacheable SQL AST creation.
	 */
	default void visitJdbcTypes(Consumer<SqlExpressableType> action, TypeConfiguration typeConfiguration) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	/**
	 * Create a DomainResult for a specific reference to this ModelPart.
	 */
	default DomainResult<?> createDomainResult(
			NavigablePath navigablePath,
			int valuesArrayPosition,
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	/**
	 * Apply SQL selections for a specific reference to this ModelPart outside the domain query's root select clause.
	 */
	default void applySqlSelections(
			NavigablePath navigablePath,
			DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
