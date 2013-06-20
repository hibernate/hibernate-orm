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
package org.hibernate.loader.plan.exec.process.internal;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessingContext;

/**
 * Identifiers are read from the ResultSet in 2 distinct phases:
 * <ol>
 *     <li>
 *         First we hydrate the identifier values (see {@link #hydrate}).  During this "phase" 2 things happen:
 *         <ol>
 *             <li>
 *                 Any "optional identifier" specified on QueryParameters is considered.  If the "optional identifier"
 *                 is to be used for this identifier read, it is used to build an EntityKey which is associated with
 *                 the {@link ResultSetProcessingContext.EntityReferenceProcessingState} for the EntityReference under
 *                 {@link ResultSetProcessingContext.EntityReferenceProcessingState#registerEntityKey}
 *             </li>
 *             <li>
 *                 All other id values are hydrated from the ResultSet.  Those hydrated values are then registered
 *                 with the {@link ResultSetProcessingContext.EntityReferenceProcessingState} for the EntityReference
 *                 under {@link ResultSetProcessingContext.EntityReferenceProcessingState#registerIdentifierHydratedForm}
 *             </li>
 *         </ol>
 *     </li>
 *     <li>
 *         Then we resolve the identifier.  This is again a 2 step process:
 *         <ol>
 *             <li>
 *                 For all fetches that "come from" an identifier (key-many-to-ones), we fully hydrate those entities
 *             </li>
 *             <li>
 *                 We then call resolve on root identifier type, and use that to build an EntityKey,which is then
 *                 registered with the {@link ResultSetProcessingContext.EntityReferenceProcessingState} for the
 *                 EntityReference whose identifier we are reading under
 *                 {@link ResultSetProcessingContext.EntityReferenceProcessingState#registerEntityKey}
 *             </li>
 *         </ol>
 *     </li>
 * </ol>
 *
 * @author Steve Ebersole
 */
public interface EntityIdentifierReader {
	/**
	 * Hydrate the entity identifier.  Perform the first phase outlined above.
	 *
	 * @param resultSet The ResultSet
	 * @param context The processing context
	 *
	 * @throws java.sql.SQLException Problem accessing ResultSet
	 */
	public void hydrate(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException;

	/**
	 * Resolve the entity identifier.  Perform the second phase outlined above.
	 *
	 * @param resultSet The ResultSet
	 * @param context The processing context
	 *
	 * @throws java.sql.SQLException Problem accessing ResultSet
	 */
	public void resolve(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException;
}
