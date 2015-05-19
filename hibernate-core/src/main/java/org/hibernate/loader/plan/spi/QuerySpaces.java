/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.spi;

import java.util.List;

/**
 * Models a collection of {@link QuerySpace} references and exposes the ability to find a {@link QuerySpace} by its UID
 * <p/>
 * todo : make this hierarchical... that would be needed to truly work for hql parser
 *
 * @author Steve Ebersole
 */
public interface QuerySpaces {
	/**
	 * Gets the root QuerySpace references.
	 *
	 * @return The roots
	 */
	public List<QuerySpace> getRootQuerySpaces();

	/**
	 * Locate a QuerySpace by its uid.
	 *
	 * @param uid The QuerySpace uid to match
	 *
	 * @return The match, {@code null} is returned if no match.
	 *
	 * @see QuerySpace#getUid()
	 */
	public QuerySpace findQuerySpaceByUid(String uid);

	/**
	 * Like {@link #findQuerySpaceByUid}, except that here an exception is thrown if the uid cannot be resolved.
	 *
	 * @param uid The uid to resolve
	 *
	 * @return The QuerySpace
	 *
	 * @throws QuerySpaceUidNotRegisteredException Rather than return {@code null}
	 */
	public QuerySpace getQuerySpaceByUid(String uid);
}
