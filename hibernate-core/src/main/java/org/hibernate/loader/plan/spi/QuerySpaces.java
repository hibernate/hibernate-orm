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
