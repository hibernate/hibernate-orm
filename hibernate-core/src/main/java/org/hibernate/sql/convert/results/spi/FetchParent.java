/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.convert.results.spi;

import java.util.List;

import org.hibernate.sql.ast.expression.domain.NavigablePath;
import org.hibernate.sql.exec.results.process.spi.InitializerParent;

/**
 * Contract for things that can be the parent of a fetch
 *
 * @author Steve Ebersole
 */
public interface FetchParent {
	/**
	 * Get the property path to this parent
	 *
	 * @return The property path
	 */
	NavigablePath getNavigablePath();

	/**
	 * Get the UID for this fetch source's query space.
	 *
	 * @return The query space UID.
	 */
	String getTableGroupUniqueIdentifier();

	InitializerParent getInitializerParentForFetchInitializers();

	void addFetch(Fetch fetch);

	/**
	 * Retrieve the fetches owned by this fetch source.
	 * <p/>
	 * This is why generics suck :(  Ideally this would override
	 * FetchSource#getFetches and give a covariant return of a List of
	 * org.hibernate.sql.convert.results.spi.Fetch
	 *
	 * @return The owned fetches.
	 */
	List<Fetch> getFetches();
}
