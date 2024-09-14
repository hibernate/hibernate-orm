/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.results.graph.entity;

import org.hibernate.engine.FetchTiming;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.spi.NavigablePath;

/**
 * Fetchable which is entity-valued
 *
 * @author Steve Ebersole
 */
public interface EntityValuedFetchable extends Fetchable, EntityValuedModelPart {
	@Override
	EntityFetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState);

	boolean isOptional();

	boolean isUnwrapProxy();
}
