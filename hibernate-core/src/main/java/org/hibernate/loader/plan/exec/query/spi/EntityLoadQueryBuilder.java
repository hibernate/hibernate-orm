/*
 * jDocBook, processing of DocBook sources
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
package org.hibernate.loader.plan.exec.query.spi;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.exec.spi.AliasResolutionContext;
import org.hibernate.loader.plan.spi.LoadPlan;

/**
 * Contract for generating the query (currently the SQL string specifically) based on a LoadPlan with a
 * single root EntityReturn
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public interface EntityLoadQueryBuilder {
	/**
	 * Generates the query for the performing load.
	 *
	 * @param loadPlan The load
	 * @param factory The session factory.
	 * @param buildingParameters Parameters influencing the building of the query
	 * @param aliasResolutionContext The alias resolution context.
	 *
	 * @return the SQL string for performing the load
	 */
	String generateSql(
			LoadPlan loadPlan,
			SessionFactoryImplementor factory,
			QueryBuildingParameters buildingParameters,
			AliasResolutionContext aliasResolutionContext);

	/**
	 * Generates the query for the performing load, based on the specified key column(s).
	 *
	 * @param keyColumnNames The names of the key columns to use
	 * @param loadPlan The load
	 * @param factory The session factory.
	 * @param buildingParameters Parameters influencing the building of the query
	 * @param aliasResolutionContext The alias resolution context.
	 *
	 * @return the SQL string for performing the load
	 */
	String generateSql(
			String[] keyColumnNames,
			LoadPlan loadPlan,
			SessionFactoryImplementor factory,
			QueryBuildingParameters buildingParameters,
			AliasResolutionContext aliasResolutionContext);
}
