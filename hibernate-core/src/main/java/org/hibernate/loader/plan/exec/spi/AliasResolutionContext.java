/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
