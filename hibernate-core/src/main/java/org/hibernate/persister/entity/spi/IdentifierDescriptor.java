/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity.spi;

import java.util.List;

import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.SingularPersistentAttribute;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public interface IdentifierDescriptor<O,J> extends Navigable<J>, SingularPersistentAttribute<O,J> {
	Type getIdType();

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
	 * "virtual" attribute mapping ({@link org.hibernate.persister.common.spi.VirtualPersistentAttribute})
	 */
	SingularPersistentAttribute<O,J> getIdAttribute();

	/**
	 * Retrieve the columns making up the identifier
	 */
	List<Column> getColumns();
}
