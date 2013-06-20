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

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.exec.internal.AliasResolutionContextImpl;
import org.hibernate.loader.plan.exec.process.internal.ResultSetProcessorImpl;
import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessor;
import org.hibernate.loader.plan.exec.query.internal.EntityLoadQueryBuilderImpl;
import org.hibernate.loader.plan.exec.query.spi.QueryBuildingParameters;
import org.hibernate.loader.plan.spi.LoadPlan;

/**
 * Wraps a LoadPlan (for an entity load) and exposes details about the query and its execution.
 *
 * @author Steve Ebersole
 */
public class LoadQueryDetails {
	private final SessionFactoryImplementor factory;
	private final LoadPlan loadPlan;

	private final AliasResolutionContext aliasResolutionContext;
	private final String sqlStatement;
	private final ResultSetProcessor resultSetProcessor;

	/**
	 * Constructs a LoadQueryDetails object from the given inputs.
	 *
	 *
	 * @param uniqueKeyColumnNames
	 * @param loadPlan The load plan
	 * @param factory The SessionFactory
	 * @param buildingParameters And influencers that would affect the generated SQL (mostly we are concerned with those
	 * that add additional joins here)
	 *
	 * @return The LoadQueryDetails
	 */
	public static LoadQueryDetails makeForBatching(
			String[] uniqueKeyColumnNames,
			LoadPlan loadPlan,
			SessionFactoryImplementor factory,
			QueryBuildingParameters buildingParameters) {
		final AliasResolutionContext aliasResolutionContext = new AliasResolutionContextImpl( factory );
		final ResultSetProcessor resultSetProcessor = new ResultSetProcessorImpl( loadPlan, false );
		final String sqlStatement = EntityLoadQueryBuilderImpl.INSTANCE.generateSql(
				uniqueKeyColumnNames,
				loadPlan,
				factory,
				buildingParameters,
				aliasResolutionContext
		);
		return new LoadQueryDetails( factory, loadPlan, aliasResolutionContext, resultSetProcessor, sqlStatement );
	}

	private LoadQueryDetails(
			SessionFactoryImplementor factory,
			LoadPlan loadPlan,
			AliasResolutionContext aliasResolutionContext,
			ResultSetProcessor resultSetProcessor,
			String sqlStatement) {
		this.factory = factory;
		this.loadPlan = loadPlan;
		this.aliasResolutionContext = aliasResolutionContext;
		this.resultSetProcessor = resultSetProcessor;
		this.sqlStatement = sqlStatement;
	}

	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	public LoadPlan getLoadPlan() {
		return loadPlan;
	}

	public AliasResolutionContext getAliasResolutionContext() {
		return aliasResolutionContext;
	}

	public String getSqlStatement() {
		return sqlStatement;
	}

	public ResultSetProcessor getResultSetProcessor() {
		return resultSetProcessor;
	}
}
