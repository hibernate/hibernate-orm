/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.envers.tools.Pair;

/**
 * Queries that allow retrieving snapshots of all entities (regardless of their particular type) changed in the given
 * revision. Note that this API can be legally used only when default mechanism of tracking modified entity names
 * is enabled.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public interface CrossTypeRevisionChangesReader {
	/**
	 * Find all entities changed (added, updated and removed) in a given revision. Executes <i>n+1</i> SQL queries,
	 * where <i>n</i> is a number of different entity classes modified within specified revision.
	 *
	 * @param revision Revision number.
	 *
	 * @return Snapshots of all audited entities changed in a given revision.
	 *
	 * @throws IllegalStateException If the associated entity manager is closed.
	 * @throws IllegalArgumentException If a revision number is <code>null</code>, less or equal to 0.
	 */
	List<Object> findEntities(Number revision) throws IllegalStateException, IllegalArgumentException;

	/**
	 * Find all entities changed (added, updated or removed) in a given revision. Executes <i>n+1</i> SQL queries,
	 * where <i>n</i> is a number of different entity classes modified within specified revision.
	 *
	 * @param revision Revision number.
	 * @param revisionType Type of modification.
	 *
	 * @return Snapshots of all audited entities changed in a given revision and filtered by modification type.
	 *
	 * @throws IllegalStateException If the associated entity manager is closed.
	 * @throws IllegalArgumentException If a revision number is {@code null}, less or equal to 0.
	 */
	List<Object> findEntities(Number revision, RevisionType revisionType) throws IllegalStateException,
			IllegalArgumentException;

	/**
	 * Find all entities changed (added, updated and removed) in a given revision grouped by modification type.
	 * Executes <i>mn+1</i> SQL queries, where:
	 * <ul>
	 * <li><i>n</i> - number of different entity classes modified within specified revision.
	 * <li><i>m</i> - number of different revision types. See {@link RevisionType} enum.
	 * </ul>
	 *
	 * @param revision Revision number.
	 *
	 * @return Map containing lists of entity snapshots grouped by modification operation (e.g. addition, update, removal).
	 *
	 * @throws IllegalStateException If the associated entity manager is closed.
	 * @throws IllegalArgumentException If a revision number is {@code null}, less or equal to 0.
	 */
	Map<RevisionType, List<Object>> findEntitiesGroupByRevisionType(Number revision)
			throws IllegalStateException,
			IllegalArgumentException;

	/**
	 * Returns set of entity names and corresponding Java classes modified in a given revision.
	 *
	 * @param revision Revision number.
	 *
	 * @return Set of entity names and corresponding Java classes modified in a given revision.
	 *
	 * @throws IllegalStateException If the associated entity manager is closed.
	 * @throws IllegalArgumentException If a revision number is {@code null}, less or equal to 0.
	 */
	Set<Pair<String, Class>> findEntityTypes(Number revision)
			throws IllegalStateException, IllegalArgumentException;
}
