/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.result.spi;

import java.util.List;

import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.exec.results.spi.InitializerParent;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableContainerReference;

/**
 * Contract for things that can be the parent of a fetch
 *
 * @author Steve Ebersole
 */
public interface FetchParent {
	NavigableContainerReference getNavigableContainerReference();

	/**
	 * Get the property path to this parent
	 *
	 * @return The property path
	 */
	NavigablePath getNavigablePath();

	InitializerParent getInitializerParentForFetchInitializers();

	void addFetch(Fetch fetch);

	/**
	 * Retrieve the fetches owned by this fetch source.
	 * <p/>
	 * This is why generics suck :(  Ideally this would override
	 * FetchSource#getFetches and give a covariant return of a List of
	 * org.hibernate.sql.ast.produce.result.spi.Fetch
	 *
	 * @return The owned fetches.
	 */
	List<Fetch> getFetches();
}
