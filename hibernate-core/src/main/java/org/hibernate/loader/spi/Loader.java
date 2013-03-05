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
package org.hibernate.loader.spi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.transform.ResultTransformer;

/**
 * Definition of the Loader contract.
 * <p/>
 * Capabilities I'd like to see added (todo):<ul>
 *     <li>
 *         expose the underlying "query" (although what I see here relies heavily on
 *         https://github.com/hibernate/hibernate-orm/wiki/Proposal---SQL-generation)
 *     </li>
 * </ul>
 *
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface Loader {
	public LoadPlan getLoadPlan();

	/**
	 * Obtain the on-demand form of this loader, if possible.
	 *
	 * @return The on-demand version of this loader
	 */
	public OnDemandLoader asOnDemandLoader();

	public List extractResults(
			ResultSet resultSet,
			SessionImplementor session,
			QueryParameters queryParameters,
			boolean returnProxies,
			ResultTransformer forcedResultTransformer) throws SQLException;
}
