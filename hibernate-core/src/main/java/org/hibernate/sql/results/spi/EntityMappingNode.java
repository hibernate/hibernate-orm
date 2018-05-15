/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.query.NavigablePath;

/**
 * Represents a reference to an entity either as a return, fetch, or collection element or index.
 *
 * @author Steve Ebersole
 */
public interface EntityMappingNode extends ResultSetMappingNode, FetchParent {
	EntityValuedNavigable getEntityValuedNavigable();

	@Override
	NavigablePath getNavigablePath();
}
