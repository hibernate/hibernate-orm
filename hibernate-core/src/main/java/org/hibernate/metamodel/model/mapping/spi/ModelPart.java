/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.mapping.spi;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;

/**
 * Describes a mapping of related to any part of the app's domain model - e.g.
 * an attribute, an entity identifier, collection elements, etc
 *
 * @see DomainResultProducer
 * @see javax.persistence.metamodel.Bindable
 *
 * @author Steve Ebersole
 */
public interface ModelPart {

	/**
	 * Create a DomainResult for a specific reference to this ModelPart.
	 */
	default <T> DomainResult<T> createDomainResult(
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

	default Writeable getWriteable() {
		// todo (6.0) : or in-line
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
