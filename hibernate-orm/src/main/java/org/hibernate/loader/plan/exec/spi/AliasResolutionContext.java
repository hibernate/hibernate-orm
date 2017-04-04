/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.spi;

/**
 * Provides aliases that are used by load queries and ResultSet processors.
 *
 * @author Gail Badner
 * @author Steve Ebersole
 */
public interface AliasResolutionContext {
	public String resolveSqlTableAliasFromQuerySpaceUid(String querySpaceUid);

	/**
	 * Resolve the given QuerySpace UID to the EntityReferenceAliases representing the SQL aliases used in
	 * building the SQL query.
	 * <p/>
	 * Assumes that a QuerySpace has already been registered.  As such this method simply returns {@code null}  if
	 * no QuerySpace with that UID has yet been resolved in the context.
	 *
	 * @param querySpaceUid The QuerySpace UID whose EntityReferenceAliases we want to look up.
	 *
	 * @return The corresponding QuerySpace UID, or {@code null}.
	 */
	public EntityReferenceAliases resolveEntityReferenceAliases(String querySpaceUid);

	/**
	 * Resolve the given QuerySpace UID to the CollectionReferenceAliases representing the SQL aliases used in
	 * building the SQL query.
	 * <p/>
	 * Assumes that a QuerySpace has already been registered.  As such this method simply returns {@code null}  if
	 * no QuerySpace with that UID has yet been resolved in the context.
	 *
	 * @param querySpaceUid The QuerySpace UID whose CollectionReferenceAliases we want to look up.
	 *
	 * @return The corresponding QuerySpace UID, or {@code null}.
	 */
	public CollectionReferenceAliases resolveCollectionReferenceAliases(String querySpaceUid);

}
