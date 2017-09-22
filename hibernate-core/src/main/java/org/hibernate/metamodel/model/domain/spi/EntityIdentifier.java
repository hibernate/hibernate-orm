/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.List;

import org.hibernate.id.IdentifierGenerator;
import org.hibernate.metamodel.model.relational.spi.Column;

/**
 * Descriptor for an entity's identifier
 * @author Steve Ebersole
 */
public interface EntityIdentifier<O,J> extends Navigable<J>, SingularPersistentAttribute<O,J>, AllowableParameterType<J> {
	String NAVIGABLE_ID = "{id}";
	String LEGACY_NAVIGABLE_ID = "id";

	/**
	 * Is this identifier defined by a single attribute on the entity?
	 * <p/>
	 * The only time this is false is in the case of a non-aggregated composite identifier.
	 *
	 * @return {@code false} indicates we have a non-aggregated composite identifier.
	 */
	boolean hasSingleIdAttribute();

	/**
	 * Get a SingularPersistentAttribute representation of the identifier.
	 * <p/>
	 * Note that for the case of a non-aggregated composite identifier this returns a
	 * "virtual" attribute mapping ({@link VirtualPersistentAttribute})
	 */
	SingularPersistentAttribute<O,J> getIdAttribute();

	IdentifierGenerator getIdentifierValueGenerator();

	/**
	 * Retrieve the columns making up the identifier
	 */
	List<Column> getColumns();
}
