/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc..
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
 *
 */
package org.hibernate.loader.plan.build.internal;


import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.GraphNodeImplementor;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;

/**
 * Loadplan building strategy for {@link javax.persistence.EntityGraph} is applied in {@code javax.persistence.loadgraph} mode.
 *
 * @author Strong Liu <stliu@hibernate.org>
 */
public class LoadGraphLoadPlanBuildingStrategy extends AbstractEntityGraphVisitationStrategy {
	private final GraphNodeImplementor rootEntityGraph;

	public LoadGraphLoadPlanBuildingStrategy(
			final SessionFactoryImplementor sessionFactory, final LoadQueryInfluencers loadQueryInfluencers,final LockMode lockMode) {
		super( sessionFactory, loadQueryInfluencers , lockMode);
		this.rootEntityGraph = (GraphNodeImplementor) loadQueryInfluencers.getLoadGraph();
	}

	@Override
	protected GraphNodeImplementor getRootEntityGraph() {
		return rootEntityGraph;
	}

	@Override
	protected FetchStrategy resolveImplicitFetchStrategyFromEntityGraph(
			final AssociationAttributeDefinition attributeDefinition) {
		FetchStrategy fetchStrategy = attributeDefinition.determineFetchPlan(
				loadQueryInfluencers,
				currentPropertyPath
		);
		if ( fetchStrategy.getTiming() == FetchTiming.IMMEDIATE && fetchStrategy.getStyle() == FetchStyle.JOIN ) {
			// see if we need to alter the join fetch to another form for any reason
			fetchStrategy = adjustJoinFetchIfNeeded( attributeDefinition, fetchStrategy );
		}

		return fetchStrategy;
	}

}
